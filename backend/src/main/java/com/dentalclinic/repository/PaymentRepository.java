package com.dentalclinic.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dentalclinic.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByAppointment_Id(UUID appointmentId);

    List<Payment> findByAppointment_Patient_IdOrderByPaidAtDesc(UUID patientId);

    List<Payment> findByMethodOrderByPaidAtDesc(String method);

    boolean existsByAppointment_Id(UUID appointmentId);
}
