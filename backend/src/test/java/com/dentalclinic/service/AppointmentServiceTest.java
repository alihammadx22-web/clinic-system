package com.dentalclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.dentalclinic.model.DoctorSchedule;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.AppointmentRepository;
import com.dentalclinic.repository.DoctorScheduleRepository;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.service.AppointmentService.AppointmentRequest;
import com.dentalclinic.service.AppointmentService.RescheduleRequest;
import com.dentalclinic.service.AppointmentService.StatusRequest;

class AppointmentServiceTest {

    private AppointmentRepository appointmentRepository;
    private DoctorScheduleRepository doctorScheduleRepository;
    private UserRepository userRepository;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentRepository = org.mockito.Mockito.mock(AppointmentRepository.class);
        doctorScheduleRepository = org.mockito.Mockito.mock(DoctorScheduleRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        appointmentService = new AppointmentService(appointmentRepository, doctorScheduleRepository, userRepository);
    }

    @Test
    void createSavesScheduledAppointmentInDoctorSchedule() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        AppointmentRequest request = request(patientId, doctorId, "2026-09-14", "09:30");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, 1))
                .thenReturn(Optional.of(schedule(doctorId, 1, "09:00", "17:00")));
        when(appointmentRepository.existsByDoctor_IdAndAppointmentDateAndStartTime(
                doctorId,
                LocalDate.parse("2026-09-14"),
                LocalTime.parse("09:30")
        )).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appointmentService.create(request, reception());

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(appointmentCaptor.capture());
        Appointment saved = appointmentCaptor.getValue();

        assertThat(saved.getPatient().getId()).isEqualTo(patientId);
        assertThat(saved.getDoctor().getId()).isEqualTo(doctorId);
        assertThat(saved.getAppointmentDate()).isEqualTo(LocalDate.parse("2026-09-14"));
        assertThat(saved.getStartTime()).isEqualTo(LocalTime.parse("09:30"));
        assertThat(saved.getStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    void createRejectsNonPatientUser() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        AppointmentRequest request = request(patientId, doctorId, "2026-09-14", "09:00");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.create(request, reception()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("patientId must reference a PATIENT user");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createRejectsNonDoctorUser() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        AppointmentRequest request = request(patientId, doctorId, "2026-09-14", "09:00");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.create(request, reception()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("doctorId must reference a DOCTOR user");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createRejectsNonThirtyMinuteSlot() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        AppointmentRequest request = request(patientId, doctorId, "2026-09-14", "09:15");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));

        assertThatThrownBy(() -> appointmentService.create(request, reception()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("appointments use fixed 30-minute slots");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createRejectsAppointmentOutsideDoctorSchedule() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        AppointmentRequest request = request(patientId, doctorId, "2026-09-14", "08:30");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, 1))
                .thenReturn(Optional.of(schedule(doctorId, 1, "09:00", "17:00")));

        assertThatThrownBy(() -> appointmentService.create(request, reception()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("appointment must be inside the doctor's schedule");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createRejectsDoubleBookedDoctorSlot() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        AppointmentRequest request = request(patientId, doctorId, "2026-09-14", "09:00");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, 1))
                .thenReturn(Optional.of(schedule(doctorId, 1, "09:00", "17:00")));
        when(appointmentRepository.existsByDoctor_IdAndAppointmentDateAndStartTime(
                doctorId,
                LocalDate.parse("2026-09-14"),
                LocalTime.parse("09:00")
        )).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.create(request, reception()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Doctor already has an appointment at this time");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void rescheduleAllowsKeepingSameSlot() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId, patientId, doctorId, "2026-09-14", "09:00");
        RescheduleRequest request = new RescheduleRequest(
                doctorId,
                LocalDate.parse("2026-09-14"),
                LocalTime.parse("09:00"),
                "Updated notes"
        );

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, 1))
                .thenReturn(Optional.of(schedule(doctorId, 1, "09:00", "17:00")));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appointmentService.reschedule(appointmentId, request);

        verify(appointmentRepository, never()).existsByDoctor_IdAndAppointmentDateAndStartTime(any(), any(), any());
        assertThat(appointment.getNotes()).isEqualTo("Updated notes");
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void rescheduleRejectsBookedDoctorSlot() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId, patientId, doctorId, "2026-09-14", "09:00");
        RescheduleRequest request = new RescheduleRequest(
                doctorId,
                LocalDate.parse("2026-09-14"),
                LocalTime.parse("09:30"),
                "Updated notes"
        );

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, 1))
                .thenReturn(Optional.of(schedule(doctorId, 1, "09:00", "17:00")));
        when(appointmentRepository.existsByDoctor_IdAndAppointmentDateAndStartTime(
                doctorId,
                LocalDate.parse("2026-09-14"),
                LocalTime.parse("09:30")
        )).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.reschedule(appointmentId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Doctor already has an appointment at this time");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void updateStatusRejectsInvalidStatus() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId, UUID.randomUUID(), UUID.randomUUID(), "2026-09-14", "09:00");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.updateStatus(appointmentId, new StatusRequest("NO_SHOW"), reception()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Status must be SCHEDULED, CHECKED_IN, IN_PROGRESS, COMPLETED, or CANCELLED");
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void updateStatusSavesAllowedStatus() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId, UUID.randomUUID(), UUID.randomUUID(), "2026-09-14", "09:00");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appointmentService.updateStatus(appointmentId, new StatusRequest("checked_in"), reception());

        assertThat(appointment.getStatus()).isEqualTo("CHECKED_IN");
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void findAllReturnsOwnAppointmentsForDoctors() {
        UUID doctorId = UUID.randomUUID();

        when(appointmentRepository.findByDoctor_IdOrderByAppointmentDateDescStartTimeDesc(doctorId)).thenReturn(List.of());

        appointmentService.findAll(doctorAuth(doctorId));

        verify(appointmentRepository).findByDoctor_IdOrderByAppointmentDateDescStartTimeDesc(doctorId);
        verify(appointmentRepository, never()).findAll();
    }

    @Test
    void doctorCannotViewAnotherDoctorsAppointment() {
        UUID doctorId = UUID.randomUUID();
        UUID otherDoctorId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = appointment(appointmentId, UUID.randomUUID(), otherDoctorId, "2026-09-14", "09:00");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.findById(appointmentId, doctorAuth(doctorId)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Doctors can view their own appointments only");
    }

    private AppointmentRequest request(UUID patientId, UUID doctorId, String date, String startTime) {
        return new AppointmentRequest(
                patientId,
                doctorId,
                LocalDate.parse(date),
                LocalTime.parse(startTime),
                "Routine visit"
        );
    }

    private Appointment appointment(UUID id, UUID patientId, UUID doctorId, String date, String startTime) {
        return Appointment.builder()
                .id(id)
                .patient(patient(patientId))
                .doctor(doctor(doctorId))
                .appointmentDate(LocalDate.parse(date))
                .startTime(LocalTime.parse(startTime))
                .status("SCHEDULED")
                .notes("Routine visit")
                .build();
    }

    private DoctorSchedule schedule(UUID doctorId, int dayOfWeek, String startTime, String endTime) {
        return DoctorSchedule.builder()
                .id(UUID.randomUUID())
                .doctor(doctor(doctorId))
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.parse(startTime))
                .endTime(LocalTime.parse(endTime))
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

    private Authentication reception() {
        return new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_RECEPTION"))
        );
    }

    private Authentication doctorAuth(UUID doctorId) {
        return new UsernamePasswordAuthenticationToken(
                doctorId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );
    }
}
