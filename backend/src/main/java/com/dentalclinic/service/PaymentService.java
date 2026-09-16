package com.dentalclinic.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Payment;
import com.dentalclinic.repository.AppointmentRepository;
import com.dentalclinic.repository.PaymentRepository;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Service
@Transactional
public class PaymentService {

    private static final Set<String> VALID_METHODS = Set.of("CASH", "CARD");

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    public PaymentService(PaymentRepository paymentRepository, AppointmentRepository appointmentRepository) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll(Authentication authentication) {
        if (hasRole(authentication, "PATIENT")) {
            return paymentRepository.findByAppointment_Patient_IdOrderByPaidAtDesc(currentUserId(authentication)).stream()
                    .map(PaymentResponse::from)
                    .toList();
        }
        return paymentRepository.findAll().stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID id, Authentication authentication) {
        Payment payment = findPayment(id);
        requireCanView(payment, authentication);
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findByAppointmentId(UUID appointmentId, Authentication authentication) {
        Appointment appointment = findAppointment(appointmentId);
        requireCanViewAppointment(appointment, authentication);
        return paymentRepository.findByAppointment_Id(appointmentId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    public PaymentResponse create(PaymentRequest request) {
        Appointment appointment = findAppointment(request.appointmentId());
        requirePositiveAmount(request.amount());
        String method = normalizeMethod(request.method());
        requireValidMethod(method);
        requireNoPaymentForAppointment(appointment.getId());

        Payment payment = Payment.builder()
                .appointment(appointment)
                .amount(request.amount())
                .method(method)
                .paidAt(Instant.now())
                .build();

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    public void delete(UUID id) {
        Payment payment = findPayment(id);
        paymentRepository.delete(payment);
    }

    private Payment findPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Payment not found"));
    }

    private Appointment findAppointment(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "payment must reference an existing appointment"));
    }

    private void requirePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "amount must be greater than 0");
        }
    }

    private String normalizeMethod(String method) {
        return method == null ? null : method.trim().toUpperCase();
    }

    private void requireValidMethod(String method) {
        if (!VALID_METHODS.contains(method)) {
            throw new ResponseStatusException(BAD_REQUEST, "method must be CASH or CARD");
        }
    }

    private void requireNoPaymentForAppointment(UUID appointmentId) {
        if (paymentRepository.existsByAppointment_Id(appointmentId)) {
            throw new ResponseStatusException(CONFLICT, "Appointment already has a payment");
        }
    }

    private void requireCanView(Payment payment, Authentication authentication) {
        requireCanViewAppointment(payment.getAppointment(), authentication);
    }

    private void requireCanViewAppointment(Appointment appointment, Authentication authentication) {
        if (hasRole(authentication, "PATIENT") && !appointment.getPatient().getId().equals(currentUserId(authentication))) {
            throw new ResponseStatusException(FORBIDDEN, "Patients can view payments for their own appointments only");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private UUID currentUserId(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(FORBIDDEN, "Authenticated user id is invalid");
        }
    }

    public record PaymentRequest(
            @NotNull UUID appointmentId,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal amount,
            @NotBlank @Size(max = 10) String method
    ) {
    }

    public record PaymentResponse(
            UUID id,
            UUID appointmentId,
            UUID patientId,
            UUID doctorId,
            BigDecimal amount,
            String method,
            Instant paidAt
    ) {
        public static PaymentResponse from(Payment payment) {
            Appointment appointment = payment.getAppointment();
            return new PaymentResponse(
                    payment.getId(),
                    appointment.getId(),
                    appointment.getPatient().getId(),
                    appointment.getDoctor().getId(),
                    payment.getAmount(),
                    payment.getMethod(),
                    payment.getPaidAt()
            );
        }
    }
}
