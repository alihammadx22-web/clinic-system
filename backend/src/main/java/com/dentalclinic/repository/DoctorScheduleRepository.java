package com.dentalclinic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dentalclinic.model.DoctorSchedule;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID> {

    List<DoctorSchedule> findByDoctor_IdOrderByDayOfWeekAsc(UUID doctorId);

    Optional<DoctorSchedule> findByDoctor_IdAndDayOfWeek(UUID doctorId, Integer dayOfWeek);

    boolean existsByDoctor_IdAndDayOfWeek(UUID doctorId, Integer dayOfWeek);
}
