package com.dentalclinic.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dentalclinic.model.DoctorSchedule;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.DoctorScheduleRepository;
import com.dentalclinic.repository.UserRepository;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Service
@Transactional
public class DoctorScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final UserRepository userRepository;

    public DoctorScheduleService(DoctorScheduleRepository doctorScheduleRepository, UserRepository userRepository) {
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> findAll() {
        return doctorScheduleRepository.findAll().stream()
                .map(DoctorScheduleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> findByDoctorId(UUID doctorId) {
        requireDoctor(doctorId);
        return doctorScheduleRepository.findByDoctor_IdOrderByDayOfWeekAsc(doctorId).stream()
                .map(DoctorScheduleResponse::from)
                .toList();
    }

    public DoctorScheduleResponse create(DoctorScheduleRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        User doctor = requireDoctor(request.doctorId());
        requireUniqueDoctorDay(request.doctorId(), request.dayOfWeek(), null);

        DoctorSchedule schedule = DoctorSchedule.builder()
                .doctor(doctor)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();

        return DoctorScheduleResponse.from(doctorScheduleRepository.save(schedule));
    }

    public DoctorScheduleResponse update(UUID id, DoctorScheduleRequest request) {
        validateTimeRange(request.startTime(), request.endTime());
        DoctorSchedule schedule = findSchedule(id);
        User doctor = requireDoctor(request.doctorId());
        requireUniqueDoctorDay(request.doctorId(), request.dayOfWeek(), id);

        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());

        return DoctorScheduleResponse.from(doctorScheduleRepository.save(schedule));
    }

    public void delete(UUID id) {
        DoctorSchedule schedule = findSchedule(id);
        doctorScheduleRepository.delete(schedule);
    }

    private DoctorSchedule findSchedule(UUID id) {
        return doctorScheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Doctor schedule not found"));
    }

    private User requireDoctor(UUID doctorId) {
        return userRepository.findByIdAndRole(doctorId, "DOCTOR")
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "doctorId must reference a DOCTOR user"));
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return;
        }
        if (!startTime.isBefore(endTime)) {
            throw new ResponseStatusException(BAD_REQUEST, "startTime must be before endTime");
        }
    }

    private void requireUniqueDoctorDay(UUID doctorId, Integer dayOfWeek, UUID currentScheduleId) {
        if (doctorId == null || dayOfWeek == null) {
            return;
        }
        doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, dayOfWeek).ifPresent(existing -> {
            if (currentScheduleId == null || !existing.getId().equals(currentScheduleId)) {
                throw new ResponseStatusException(CONFLICT, "Doctor already has a schedule for this day");
            }
        });
    }

    public record DoctorScheduleRequest(
            @NotNull UUID doctorId,
            @NotNull @Min(0) @Max(6) Integer dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime
    ) {
    }

    public record DoctorScheduleResponse(
            UUID id,
            UUID doctorId,
            Integer dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        public static DoctorScheduleResponse from(DoctorSchedule schedule) {
            return new DoctorScheduleResponse(
                    schedule.getId(),
                    schedule.getDoctor().getId(),
                    schedule.getDayOfWeek(),
                    schedule.getStartTime(),
                    schedule.getEndTime()
            );
        }
    }
}
