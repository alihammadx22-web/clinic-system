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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dentalclinic.service.PaymentService;
import com.dentalclinic.service.PaymentService.PaymentRequest;
import com.dentalclinic.service.PaymentService.PaymentResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponse> findAll(Authentication authentication) {
        return paymentService.findAll(authentication);
    }

    @GetMapping("/{id}")
    public PaymentResponse findById(@PathVariable UUID id, Authentication authentication) {
        return paymentService.findById(id, authentication);
    }

    @GetMapping("/appointment/{appointmentId}")
    public List<PaymentResponse> findByAppointmentId(@PathVariable UUID appointmentId, Authentication authentication) {
        return paymentService.findByAppointmentId(appointmentId, authentication);
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse payment = paymentService.create(request);
        return ResponseEntity.created(URI.create("/api/payments/" + payment.id())).body(payment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
