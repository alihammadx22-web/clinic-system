package com.dentalclinic.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dentalclinic.service.DentalCaseService;
import com.dentalclinic.service.DentalCaseService.DentalCaseRequest;
import com.dentalclinic.service.DentalCaseService.DentalCaseResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/dental-cases")
public class DentalCaseController {

    private final DentalCaseService dentalCaseService;

    public DentalCaseController(DentalCaseService dentalCaseService) {
        this.dentalCaseService = dentalCaseService;
    }

    @GetMapping
    public List<DentalCaseResponse> findAll(Authentication authentication) {
        return dentalCaseService.findAll(authentication);
    }

    @GetMapping("/{id}")
    public DentalCaseResponse findById(@PathVariable UUID id, Authentication authentication) {
        return dentalCaseService.findById(id, authentication);
    }

    @GetMapping("/patient/{patientId}")
    public List<DentalCaseResponse> findByPatientId(@PathVariable UUID patientId, Authentication authentication) {
        return dentalCaseService.findByPatientId(patientId, authentication);
    }

    @PostMapping
    public ResponseEntity<DentalCaseResponse> create(@Valid @RequestBody DentalCaseRequest request) {
        DentalCaseResponse dentalCase = dentalCaseService.create(request);
        return ResponseEntity.created(URI.create("/api/dental-cases/" + dentalCase.id())).body(dentalCase);
    }

    @PutMapping("/{id}")
    public DentalCaseResponse update(@PathVariable UUID id, @Valid @RequestBody DentalCaseRequest request) {
        return dentalCaseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dentalCaseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
