package com.dentalclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

import com.dentalclinic.model.DentalCase;
import com.dentalclinic.model.User;
import com.dentalclinic.repository.DentalCaseRepository;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.service.DentalCaseService.DentalCaseRequest;

class DentalCaseServiceTest {

    private DentalCaseRepository dentalCaseRepository;
    private UserRepository userRepository;
    private DentalCaseService dentalCaseService;

    @BeforeEach
    void setUp() {
        dentalCaseRepository = org.mockito.Mockito.mock(DentalCaseRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        dentalCaseService = new DentalCaseService(dentalCaseRepository, userRepository);
    }

    @Test
    void createSavesCaseForPatientAndDoctorUsers() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        DentalCaseRequest request = request(patientId, doctorId, "ORTHO", "open");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(dentalCaseRepository.save(any(DentalCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dentalCaseService.create(request);

        ArgumentCaptor<DentalCase> caseCaptor = ArgumentCaptor.forClass(DentalCase.class);
        verify(dentalCaseRepository).save(caseCaptor.capture());
        DentalCase saved = caseCaptor.getValue();

        assertThat(saved.getPatient().getId()).isEqualTo(patientId);
        assertThat(saved.getDoctor().getId()).isEqualTo(doctorId);
        assertThat(saved.getCaseType()).isEqualTo("ORTHO");
        assertThat(saved.getStatus()).isEqualTo("OPEN");
        assertThat(saved.getCompletedAt()).isNull();
    }

    @Test
    void createRejectsNonPatientUser() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        DentalCaseRequest request = request(patientId, doctorId, "ORTHO", "OPEN");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dentalCaseService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("patientId must reference a PATIENT user");
        verify(dentalCaseRepository, never()).save(any());
    }

    @Test
    void createRejectsNonDoctorUser() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        DentalCaseRequest request = request(patientId, doctorId, "ORTHO", "OPEN");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dentalCaseService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("doctorId must reference a DOCTOR user");
        verify(dentalCaseRepository, never()).save(any());
    }

    @Test
    void createRejectsInvalidStatus() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        DentalCaseRequest request = request(patientId, doctorId, "ORTHO", "CANCELLED");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));

        assertThatThrownBy(() -> dentalCaseService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Status must be OPEN, IN_PROGRESS, or COMPLETED");
        verify(dentalCaseRepository, never()).save(any());
    }

    @Test
    void createSetsCompletedAtForCompletedStatus() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        DentalCaseRequest request = request(patientId, doctorId, "ORTHO", "COMPLETED");

        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(dentalCaseRepository.save(any(DentalCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DentalCaseService.DentalCaseResponse response = dentalCaseService.create(request);

        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void updateSetsCompletedAtWhenStatusBecomesCompleted() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        DentalCase existing = dentalCase(caseId, patientId, doctorId, "OPEN", null);
        DentalCaseRequest request = request(patientId, doctorId, "RESTORATIVE", "COMPLETED");

        when(dentalCaseRepository.findById(caseId)).thenReturn(Optional.of(existing));
        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(dentalCaseRepository.save(any(DentalCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dentalCaseService.update(caseId, request);

        assertThat(existing.getStatus()).isEqualTo("COMPLETED");
        assertThat(existing.getCompletedAt()).isNotNull();
        verify(dentalCaseRepository).save(existing);
    }

    @Test
    void updateClearsCompletedAtWhenStatusMovesAwayFromCompleted() {
        UUID caseId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        DentalCase existing = dentalCase(caseId, patientId, doctorId, "COMPLETED", Instant.now());
        DentalCaseRequest request = request(patientId, doctorId, "RESTORATIVE", "IN_PROGRESS");

        when(dentalCaseRepository.findById(caseId)).thenReturn(Optional.of(existing));
        when(userRepository.findByIdAndRole(patientId, "PATIENT")).thenReturn(Optional.of(patient(patientId)));
        when(userRepository.findByIdAndRole(doctorId, "DOCTOR")).thenReturn(Optional.of(doctor(doctorId)));
        when(dentalCaseRepository.save(any(DentalCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dentalCaseService.update(caseId, request);

        assertThat(existing.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(existing.getCompletedAt()).isNull();
    }

    @Test
    void findAllReturnsOwnCasesForPatients() {
        UUID patientId = UUID.randomUUID();

        when(dentalCaseRepository.findByPatient_IdOrderByCreatedAtDesc(patientId)).thenReturn(List.of());

        dentalCaseService.findAll(patientAuth(patientId));

        verify(dentalCaseRepository).findByPatient_IdOrderByCreatedAtDesc(patientId);
        verify(dentalCaseRepository, never()).findAll();
    }

    @Test
    void patientCannotViewAnotherPatientsCase() {
        UUID patientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();
        DentalCase dentalCase = dentalCase(caseId, otherPatientId, UUID.randomUUID(), "OPEN", null);

        when(dentalCaseRepository.findById(caseId)).thenReturn(Optional.of(dentalCase));

        assertThatThrownBy(() -> dentalCaseService.findById(caseId, patientAuth(patientId)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Patients can view their own cases only");
    }

    @Test
    void patientCannotQueryAnotherPatientsCases() {
        UUID patientId = UUID.randomUUID();
        UUID otherPatientId = UUID.randomUUID();

        when(userRepository.findByIdAndRole(otherPatientId, "PATIENT")).thenReturn(Optional.of(patient(otherPatientId)));

        assertThatThrownBy(() -> dentalCaseService.findByPatientId(otherPatientId, patientAuth(patientId)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Patients can view their own cases only");
    }

    @Test
    void deleteRemovesExistingCase() {
        UUID caseId = UUID.randomUUID();
        DentalCase dentalCase = dentalCase(caseId, UUID.randomUUID(), UUID.randomUUID(), "OPEN", null);

        when(dentalCaseRepository.findById(caseId)).thenReturn(Optional.of(dentalCase));

        dentalCaseService.delete(caseId);

        verify(dentalCaseRepository).delete(dentalCase);
    }

    private DentalCaseRequest request(UUID patientId, UUID doctorId, String caseType, String status) {
        return new DentalCaseRequest(
                patientId,
                doctorId,
                caseType,
                "Clinical notes",
                status
        );
    }

    private DentalCase dentalCase(UUID id, UUID patientId, UUID doctorId, String status, Instant completedAt) {
        return DentalCase.builder()
                .id(id)
                .patient(patient(patientId))
                .doctor(doctor(doctorId))
                .caseType("ORTHO")
                .notes("Clinical notes")
                .status(status)
                .createdAt(Instant.now())
                .completedAt(completedAt)
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

    private Authentication patientAuth(UUID patientId) {
        return new UsernamePasswordAuthenticationToken(
                patientId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );
    }
}
