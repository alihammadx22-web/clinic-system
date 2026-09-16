package com.dentalclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Payment;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.AppointmentRepository;
import com.dentalclinic.repository.PaymentRepository;
import com.dentalclinic.service.PaymentService.PaymentRequest;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private AppointmentRepository appointmentRepository;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = org.mockito.Mockito.mock(PaymentRepository.class);
        appointmentRepository = org.mockito.Mockito.mock(AppointmentRepository.class);
        paymentService = new PaymentService(paymentRepository, appointmentRepository);
    }

    @Test
    void createSavesPaymentForExistingAppointment() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId, UUID.randomUUID(), UUID.randomUUID());
        PaymentRequest request = new PaymentRequest(appointmentId, new BigDecimal("125.00"), "cash");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(paymentRepository.existsByAppointment_Id(appointmentId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentService.PaymentResponse response = paymentService.create(request);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();

        assertThat(saved.getAppointment()).isSameAs(appointment);
        assertThat(saved.getAmount()).isEqualByComparingTo("125.00");
        assertThat(saved.getMethod()).isEqualTo("CASH");
        assertThat(saved.getPaidAt()).isNotNull();
        assertThat(response.paidAt()).isEqualTo(saved.getPaidAt());
    }

    @Test
    void createRejectsMissingAppointment() {
        UUID appointmentId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(appointmentId, new BigDecimal("125.00"), "CASH");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("payment must reference an existing appointment");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsNonPositiveAmount() {
        UUID appointmentId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(appointmentId, BigDecimal.ZERO, "CASH");

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, UUID.randomUUID(), UUID.randomUUID())));

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("amount must be greater than 0");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsInvalidMethod() {
        UUID appointmentId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(appointmentId, new BigDecimal("125.00"), "CHECK");

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, UUID.randomUUID(), UUID.randomUUID())));

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("method must be CASH or CARD");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateAppointmentPayment() {
        UUID appointmentId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(appointmentId, new BigDecimal("125.00"), "CARD");

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(appointment(appointmentId, UUID.randomUUID(), UUID.randomUUID())));
        when(paymentRepository.existsByAppointment_Id(appointmentId)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Appointment already has a payment");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void findAllReturnsOwnPaymentsForPatients() {
        UUID patientId = UUID.randomUUID();

        when(paymentRepository.findByAppointment_Patient_IdOrderByPaidAtDesc(patientId)).thenReturn(List.of());

        paymentService.findAll(patientAuth(patientId));

        verify(paymentRepository).findByAppointment_Patient_IdOrderByPaidAtDesc(patientId);
        verify(paymentRepository, never()).findAll();
    }

    @Test
    void patientCannotViewAnotherPatientsPayment() {
        UUID patientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(paymentId, appointment(UUID.randomUUID(), otherPatientId, UUID.randomUUID()));

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.findById(paymentId, patientAuth(patientId)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Patients can view payments for their own appointments only");
    }

    @Test
    void patientCannotViewPaymentsForAnotherPatientsAppointment() {
        UUID patientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId, otherPatientId, UUID.randomUUID());

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> paymentService.findByAppointmentId(appointmentId, patientAuth(patientId)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Patients can view payments for their own appointments only");
    }

    @Test
    void deleteRemovesExistingPayment() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(paymentId, appointment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        paymentService.delete(paymentId);

        verify(paymentRepository).delete(payment);
    }

    private Payment payment(UUID id, Appointment appointment) {
        return Payment.builder()
                .id(id)
                .appointment(appointment)
                .amount(new BigDecimal("125.00"))
                .method("CASH")
                .paidAt(Instant.now())
                .build();
    }

    private Appointment appointment(UUID id, UUID patientId, UUID doctorId) {
        return Appointment.builder()
                .id(id)
                .patient(patient(patientId))
                .doctor(doctor(doctorId))
                .appointmentDate(LocalDate.parse("2026-09-14"))
                .startTime(LocalTime.parse("09:00"))
                .status("COMPLETED")
                .build();
    }

    private User patient(UUID id) {
        return User.builder()
                .id(id)
                .fullName("Patient User")
                .email("patient@example.com")
                .phone("555-0100")
                .passwordHash("hash")
                .role("PATIENT")
                .active(true)
                .build();
    }

    private User doctor(UUID id) {
        return User.builder()
                .id(id)
                .fullName("Doctor User")
                .email("doctor@example.com")
                .phone("555-0101")
                .passwordHash("hash")
                .role("DOCTOR")
                .active(true)
                .build();
    }

    private Authentication patientAuth(UUID patientId) {
        return new UsernamePasswordAuthenticationToken(
                patientId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );
    }
}
