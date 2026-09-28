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

import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.service.AppointmentService.AppointmentRequest;
import com.dentalclinic.service.AppointmentService.AppointmentResponse;
import com.dentalclinic.service.AppointmentService.RescheduleRequest;
import com.dentalclinic.service.AppointmentService.StatusRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<AppointmentResponse> findAll(Authentication authentication) {
        return appointmentService.findAll(authentication);
    }

    @GetMapping("/{id}")
    public AppointmentResponse findById(@PathVariable UUID id, Authentication authentication) {
        return appointmentService.findById(id, authentication);
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody AppointmentRequest request,
            Authentication authentication
    ) {
        AppointmentResponse appointment = appointmentService.create(request, authentication);
        return ResponseEntity.created(URI.create("/api/appointments/" + appointment.id())).body(appointment);
    }

    @PutMapping("/{id}/reschedule")
    public AppointmentResponse reschedule(@PathVariable UUID id, @Valid @RequestBody RescheduleRequest request) {
        return appointmentService.reschedule(id, request);
    }

    @PutMapping("/{id}/status")
    public AppointmentResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusRequest request,
            Authentication authentication
    ) {
        return appointmentService.updateStatus(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
