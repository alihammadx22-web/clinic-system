package com.dentalclinic.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dentalclinic.model.DentalCase;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.DentalCaseRepository;
import com.dentalclinic.repository.UserRepository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Service
@Transactional
public class DentalCaseService {

    private static final Set<String> VALID_STATUSES = Set.of("OPEN", "IN_PROGRESS", "COMPLETED");

    private final DentalCaseRepository dentalCaseRepository;
    private final UserRepository userRepository;

    public DentalCaseService(DentalCaseRepository dentalCaseRepository, UserRepository userRepository) {
        this.dentalCaseRepository = dentalCaseRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DentalCaseResponse> findAll(Authentication authentication) {
        if (hasRole(authentication, "PATIENT")) {
            return dentalCaseRepository.findByPatient_IdOrderByCreatedAtDesc(currentUserId(authentication)).stream()
                    .map(DentalCaseResponse::from)
                    .toList();
        }
        return dentalCaseRepository.findAll().stream()
                .map(DentalCaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DentalCaseResponse findById(UUID id, Authentication authentication) {
        DentalCase dentalCase = findCase(id);
        requireCanView(dentalCase, authentication);
        return DentalCaseResponse.from(dentalCase);
    }

    @Transactional(readOnly = true)
    public List<DentalCaseResponse> findByPatientId(UUID patientId, Authentication authentication) {
        requirePatient(patientId);
        if (hasRole(authentication, "PATIENT") && !patientId.equals(currentUserId(authentication))) {
            throw new ResponseStatusException(FORBIDDEN, "Patients can view their own cases only");
        }
        return dentalCaseRepository.findByPatient_IdOrderByCreatedAtDesc(patientId).stream()
                .map(DentalCaseResponse::from)
                .toList();
    }

    public DentalCaseResponse create(DentalCaseRequest request) {
        User patient = requirePatient(request.patientId());
        User doctor = requireDoctor(request.doctorId());
        String status = normalizeStatus(request.status());
        requireValidStatus(status);

        DentalCase dentalCase = DentalCase.builder()
                .patient(patient)
                .doctor(doctor)
                .caseType(request.caseType())
                .notes(request.notes())
                .status(status)
                .completedAt(completedAtForStatus(status, null))
                .build();

        return DentalCaseResponse.from(dentalCaseRepository.save(dentalCase));
    }

    public DentalCaseResponse update(UUID id, DentalCaseRequest request) {
        DentalCase dentalCase = findCase(id);
        User patient = requirePatient(request.patientId());
        User doctor = requireDoctor(request.doctorId());
        String status = normalizeStatus(request.status());
        requireValidStatus(status);

        dentalCase.setPatient(patient);
        dentalCase.setDoctor(doctor);
        dentalCase.setCaseType(request.caseType());
        dentalCase.setNotes(request.notes());
        dentalCase.setStatus(status);
        dentalCase.setCompletedAt(completedAtForStatus(status, dentalCase.getCompletedAt()));

        return DentalCaseResponse.from(dentalCaseRepository.save(dentalCase));
    }

    public void delete(UUID id) {
        DentalCase dentalCase = findCase(id);
        dentalCaseRepository.delete(dentalCase);
    }

    private DentalCase findCase(UUID id) {
        return dentalCaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dental case not found"));
    }

    private User requirePatient(UUID patientId) {
        return userRepository.findByIdAndRole(patientId, "PATIENT")
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "patientId must reference a PATIENT user"));
    }

    private User requireDoctor(UUID doctorId) {
        return userRepository.findByIdAndRole(doctorId, "DOCTOR")
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "doctorId must reference a DOCTOR user"));
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }

    private void requireValidStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new ResponseStatusException(BAD_REQUEST, "Status must be OPEN, IN_PROGRESS, or COMPLETED");
        }
    }

    private Instant completedAtForStatus(String status, Instant currentCompletedAt) {
        if ("COMPLETED".equals(status)) {
            return currentCompletedAt == null ? Instant.now() : currentCompletedAt;
        }
        return null;
    }

    private void requireCanView(DentalCase dentalCase, Authentication authentication) {
        if (hasRole(authentication, "PATIENT") && !dentalCase.getPatient().getId().equals(currentUserId(authentication))) {
            throw new ResponseStatusException(FORBIDDEN, "Patients can view their own cases only");
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

    public record DentalCaseRequest(
            @NotNull UUID patientId,
            @NotNull UUID doctorId,
            @NotBlank @Size(max = 40) String caseType,
            String notes,
            @NotBlank @Size(max = 20) String status
    ) {
    }

    public record DentalCaseResponse(
            UUID id,
            UUID patientId,
            UUID doctorId,
            String caseType,
            String notes,
            String status,
            Instant createdAt,
            Instant completedAt
    ) {
        public static DentalCaseResponse from(DentalCase dentalCase) {
            return new DentalCaseResponse(
                    dentalCase.getId(),
                    dentalCase.getPatient().getId(),
                    dentalCase.getDoctor().getId(),
                    dentalCase.getCaseType(),
                    dentalCase.getNotes(),
                    dentalCase.getStatus(),
                    dentalCase.getCreatedAt(),
                    dentalCase.getCompletedAt()
            );
        }
    }
}
