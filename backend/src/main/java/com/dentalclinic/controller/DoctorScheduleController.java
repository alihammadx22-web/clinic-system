package com.dentalclinic.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dentalclinic.service.DoctorScheduleService;
import com.dentalclinic.service.DoctorScheduleService.DoctorScheduleRequest;
import com.dentalclinic.service.DoctorScheduleService.DoctorScheduleResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/doctor-schedules")
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    public DoctorScheduleController(DoctorScheduleService doctorScheduleService) {
        this.doctorScheduleService = doctorScheduleService;
    }

    @GetMapping
    public List<DoctorScheduleResponse> findAll() {
        return doctorScheduleService.findAll();
    }

    @GetMapping("/doctor/{doctorId}")
    public List<DoctorScheduleResponse> findByDoctorId(@PathVariable UUID doctorId) {
        return doctorScheduleService.findByDoctorId(doctorId);
    }

    @PostMapping
    public ResponseEntity<DoctorScheduleResponse> create(@Valid @RequestBody DoctorScheduleRequest request) {
        DoctorScheduleResponse schedule = doctorScheduleService.create(request);
        return ResponseEntity.created(URI.create("/api/doctor-schedules/" + schedule.id())).body(schedule);
    }

    @PutMapping("/{id}")
    public DoctorScheduleResponse update(@PathVariable UUID id, @Valid @RequestBody DoctorScheduleRequest request) {
        return doctorScheduleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        doctorScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
