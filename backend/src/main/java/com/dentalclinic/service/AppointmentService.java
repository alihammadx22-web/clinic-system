package com.dentalclinic.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.DoctorSchedule;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.AppointmentRepository;
import com.dentalclinic.repository.DoctorScheduleRepository;
import com.dentalclinic.repository.UserRepository;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Service
@Transactional
public class AppointmentService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "SCHEDULED",
            "CHECKED_IN",
            "IN_PROGRESS",
            "COMPLETED",
            "CANCELLED"
    );

    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final UserRepository userRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            DoctorScheduleRepository doctorScheduleRepository,
            UserRepository userRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAll(Authentication authentication) {
        if (hasRole(authentication, "PATIENT")) {
            return appointmentRepository.findByPatient_IdOrderByAppointmentDateDescStartTimeDesc(currentUserId(authentication)).stream()
                    .map(AppointmentResponse::from)
                    .toList();
        }
        return appointmentRepository.findAll().stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(UUID id, Authentication authentication) {
        Appointment appointment = findAppointment(id);
        requireCanView(appointment, authentication);
        return AppointmentResponse.from(appointment);
    }

    public AppointmentResponse create(AppointmentRequest request, Authentication authentication) {
        User patient = requirePatient(request.patientId());
        User doctor = requireDoctor(request.doctorId());
        requirePatientCreatesOwnAppointment(patient.getId(), authentication);
        validateSlot(request.appointmentDate(), request.startTime());
        requireInsideDoctorSchedule(doctor.getId(), request.appointmentDate(), request.startTime());
        requireOpenSlot(doctor.getId(), request.appointmentDate(), request.startTime());

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(request.appointmentDate())
                .startTime(request.startTime())
                .status("SCHEDULED")
                .notes(request.notes())
                .build();

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    public AppointmentResponse reschedule(UUID id, RescheduleRequest request) {
        Appointment appointment = findAppointment(id);
        User doctor = requireDoctor(request.doctorId());
        validateSlot(request.appointmentDate(), request.startTime());
        requireInsideDoctorSchedule(doctor.getId(), request.appointmentDate(), request.startTime());

        boolean sameSlot = appointment.getDoctor().getId().equals(doctor.getId())
                && appointment.getAppointmentDate().equals(request.appointmentDate())
                && appointment.getStartTime().equals(request.startTime());
        if (!sameSlot) {
            requireOpenSlot(doctor.getId(), request.appointmentDate(), request.startTime());
        }

        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setStartTime(request.startTime());
        appointment.setNotes(request.notes());

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    public AppointmentResponse updateStatus(UUID id, StatusRequest request) {
        Appointment appointment = findAppointment(id);
        String status = normalizeStatus(request.status());
        requireValidStatus(status);
        appointment.setStatus(status);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    public void delete(UUID id) {
        Appointment appointment = findAppointment(id);
        appointmentRepository.delete(appointment);
    }

    private Appointment findAppointment(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Appointment not found"));
    }

    private User requirePatient(UUID patientId) {
        return userRepository.findByIdAndRole(patientId, "PATIENT")
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "patientId must reference a PATIENT user"));
    }

    private User requireDoctor(UUID doctorId) {
        return userRepository.findByIdAndRole(doctorId, "DOCTOR")
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "doctorId must reference a DOCTOR user"));
    }

    private void validateSlot(LocalDate appointmentDate, LocalTime startTime) {
        if (appointmentDate == null || startTime == null) {
            return;
        }
        if (startTime.getMinute() != 0 && startTime.getMinute() != 30) {
            throw new ResponseStatusException(BAD_REQUEST, "appointments use fixed 30-minute slots");
        }
        if (startTime.getSecond() != 0 || startTime.getNano() != 0) {
            throw new ResponseStatusException(BAD_REQUEST, "appointments use fixed 30-minute slots");
        }
    }

    private void requireInsideDoctorSchedule(UUID doctorId, LocalDate appointmentDate, LocalTime startTime) {
        if (doctorId == null || appointmentDate == null || startTime == null) {
            return;
        }
        int dayOfWeek = appointmentDate.getDayOfWeek().getValue() % 7;
        DoctorSchedule schedule = doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, dayOfWeek)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "appointment must be inside the doctor's schedule"));

        LocalTime endTime = startTime.plusMinutes(30);
        if (startTime.isBefore(schedule.getStartTime()) || endTime.isAfter(schedule.getEndTime())) {
            throw new ResponseStatusException(BAD_REQUEST, "appointment must be inside the doctor's schedule");
        }
    }

    private void requireOpenSlot(UUID doctorId, LocalDate appointmentDate, LocalTime startTime) {
        if (appointmentRepository.existsByDoctor_IdAndAppointmentDateAndStartTime(doctorId, appointmentDate, startTime)) {
            throw new ResponseStatusException(CONFLICT, "Doctor already has an appointment at this time");
        }
    }

    private void requireValidStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new ResponseStatusException(BAD_REQUEST, "Status must be SCHEDULED, CHECKED_IN, IN_PROGRESS, COMPLETED, or CANCELLED");
        }
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }

    private void requirePatientCreatesOwnAppointment(UUID patientId, Authentication authentication) {
        if (hasRole(authentication, "PATIENT") && !patientId.equals(currentUserId(authentication))) {
            throw new ResponseStatusException(FORBIDDEN, "Patients can create their own appointments only");
        }
    }

    private void requireCanView(Appointment appointment, Authentication authentication) {
        if (hasRole(authentication, "PATIENT") && !appointment.getPatient().getId().equals(currentUserId(authentication))) {
            throw new ResponseStatusException(FORBIDDEN, "Patients can view their own appointments only");
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

    public record AppointmentRequest(
            @NotNull UUID patientId,
            @NotNull UUID doctorId,
            @NotNull LocalDate appointmentDate,
            @NotNull LocalTime startTime,
            String notes
    ) {
    }

    public record RescheduleRequest(
            @NotNull UUID doctorId,
            @NotNull LocalDate appointmentDate,
            @NotNull LocalTime startTime,
            String notes
    ) {
    }

    public record StatusRequest(
            @NotNull @Size(max = 20) String status
    ) {
    }

    public record AppointmentResponse(
            UUID id,
            UUID patientId,
            UUID doctorId,
            LocalDate appointmentDate,
            LocalTime startTime,
            String status,
            String notes
    ) {
        public static AppointmentResponse from(Appointment appointment) {
            return new AppointmentResponse(
                    appointment.getId(),
                    appointment.getPatient().getId(),
                    appointment.getDoctor().getId(),
                    appointment.getAppointmentDate(),
                    appointment.getStartTime(),
                    appointment.getStatus(),
                    appointment.getNotes()
            );
        }
    }
}
