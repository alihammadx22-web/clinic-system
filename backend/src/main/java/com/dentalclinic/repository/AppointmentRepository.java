package com.dentalclinic.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dentalclinic.model.Appointment;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByAppointmentDateOrderByStartTimeAsc(LocalDate appointmentDate);

    List<Appointment> findByDoctor_IdAndAppointmentDateOrderByStartTimeAsc(UUID doctorId, LocalDate appointmentDate);

    List<Appointment> findByPatient_IdOrderByAppointmentDateDescStartTimeDesc(UUID patientId);

    List<Appointment> findByDoctor_IdOrderByAppointmentDateDescStartTimeDesc(UUID doctorId);

    List<Appointment> findByStatusOrderByAppointmentDateAscStartTimeAsc(String status);

    boolean existsByDoctor_IdAndAppointmentDateAndStartTime(UUID doctorId, LocalDate appointmentDate, LocalTime startTime);
}
