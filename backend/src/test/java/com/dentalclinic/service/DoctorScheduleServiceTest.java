package com.dentalclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.dentalclinic.model.DoctorSchedule;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.DoctorScheduleRepository;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.service.DoctorScheduleService.DoctorScheduleRequest;

class DoctorScheduleServiceTest {

    private DoctorScheduleRepository doctorScheduleRepository;
    private UserRepository userRepository;
    private DoctorScheduleService doctorScheduleService;

    @BeforeEach
    void setUp() {
        doctorScheduleRepository = org.mockito.Mockito.mock(DoctorScheduleRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        doctorScheduleService = new DoctorScheduleService(doctorScheduleRepository, userRepository);
    }

    @Test
    void createSavesScheduleForDoctorUser() {
        UUID doctorId = UUID.randomUUID();
        User doctor = doctor(doctorId);
        DoctorScheduleRequest request = request(doctorId, 1, "09:00", "17:00");

        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, 1)).thenReturn(Optional.empty());
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doctorScheduleService.create(request);

        ArgumentCaptor<DoctorSchedule> scheduleCaptor = ArgumentCaptor.forClass(DoctorSchedule.class);
        verify(doctorScheduleRepository).save(scheduleCaptor.capture());
        DoctorSchedule saved = scheduleCaptor.getValue();

        assertThat(saved.getDoctor()).isSameAs(doctor);
        assertThat(saved.getDayOfWeek()).isEqualTo(1);
        assertThat(saved.getStartTime()).isEqualTo(LocalTime.parse("09:00"));
        assertThat(saved.getEndTime()).isEqualTo(LocalTime.parse("17:00"));
    }

    @Test
    void createRejectsNonDoctorUser() {
        UUID userId = UUID.randomUUID();
        DoctorScheduleRequest request = request(userId, 2, "09:00", "17:00");

        when(userRepository.findByIdAndRole(userId, "DOCTOR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorScheduleService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("doctorId must reference a DOCTOR user");
        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void createRejectsStartTimeAfterEndTime() {
        UUID doctorId = UUID.randomUUID();
        DoctorScheduleRequest request = request(doctorId, 3, "17:00", "09:00");

        assertThatThrownBy(() -> doctorScheduleService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("startTime must be before endTime");
        verify(userRepository, never()).findByIdAndRole(any(), any());
        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateDoctorDay() {
        UUID doctorId = UUID.randomUUID();
        UUID existingScheduleId = UUID.randomUUID();
        DoctorScheduleRequest request = request(doctorId, 4, "09:00", "17:00");

        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, 4))
                .thenReturn(Optional.of(schedule(existingScheduleId, doctor(doctorId), 4)));

        assertThatThrownBy(() -> doctorScheduleService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Doctor already has a schedule for this day");
        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void updateAllowsKeepingTheSameDoctorAndDay() {
        UUID doctorId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        User doctor = doctor(doctorId);
        DoctorSchedule existing = schedule(scheduleId, doctor, 5);
        DoctorScheduleRequest request = request(doctorId, 5, "10:00", "16:00");

        when(doctorScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(existing));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, 5)).thenReturn(Optional.of(existing));
        when(doctorScheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doctorScheduleService.update(scheduleId, request);

        assertThat(existing.getStartTime()).isEqualTo(LocalTime.parse("10:00"));
        assertThat(existing.getEndTime()).isEqualTo(LocalTime.parse("16:00"));
        verify(doctorScheduleRepository).save(existing);
    }

    @Test
    void updateRejectsDuplicateDoctorDayFromAnotherSchedule() {
        UUID doctorId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UUID conflictingScheduleId = UUID.randomUUID();
        User doctor = doctor(doctorId);
        DoctorSchedule existing = schedule(scheduleId, doctor, 0);
        DoctorSchedule conflicting = schedule(conflictingScheduleId, doctor, 6);
        DoctorScheduleRequest request = request(doctorId, 6, "09:00", "17:00");

        when(doctorScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(existing));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor));
        when(doctorScheduleRepository.findByDoctor_IdAndDayOfWeek(doctorId, 6)).thenReturn(Optional.of(conflicting));

        assertThatThrownBy(() -> doctorScheduleService.update(scheduleId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Doctor already has a schedule for this day");
        verify(doctorScheduleRepository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingSchedule() {
        UUID doctorId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        DoctorSchedule schedule = schedule(scheduleId, doctor(doctorId), 1);

        when(doctorScheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));

        doctorScheduleService.delete(scheduleId);

        verify(doctorScheduleRepository).delete(schedule);
    }

    private DoctorScheduleRequest request(UUID doctorId, int dayOfWeek, String startTime, String endTime) {
        return new DoctorScheduleRequest(
                doctorId,
                dayOfWeek,
                LocalTime.parse(startTime),
                LocalTime.parse(endTime)
        );
    }

    private User doctor(UUID id) {
        return User.builder()
                .id(id)
                .fullName("Doctor User")
                .email("doctor@example.com")
                .phone("555-0100")
                .passwordHash("hash")
                .role("DOCTOR")
                .active(true)
                .build();
    }

    private DoctorSchedule schedule(UUID id, User doctor, int dayOfWeek) {
        return DoctorSchedule.builder()
                .id(id)
                .doctor(doctor)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.parse("09:00"))
                .endTime(LocalTime.parse("17:00"))
                .build();
    }
}
