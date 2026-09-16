package com.dentalclinic.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dentalclinic.model.DentalCase;

public interface DentalCaseRepository extends JpaRepository<DentalCase, UUID> {

    List<DentalCase> findByPatient_IdOrderByCreatedAtDesc(UUID patientId);

    List<DentalCase> findByDoctor_IdOrderByCreatedAtDesc(UUID doctorId);

    List<DentalCase> findByStatusOrderByCreatedAtDesc(String status);

    List<DentalCase> findByCaseTypeOrderByCreatedAtDesc(String caseType);
}
