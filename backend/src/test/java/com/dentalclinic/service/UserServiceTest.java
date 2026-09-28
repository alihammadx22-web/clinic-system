package com.dentalclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.service.UserService.UserRequest;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        userService = new UserService(userRepository, new BCryptPasswordEncoder());
    }

    @Test
    void createRejectsInvalidRole() {
        UserRequest request = new UserRequest(
                "Clinic Owner",
                "owner@example.com",
                "555-0101",
                "secret123",
                "ADMIN",
                null,
                null,
                null,
                null,
                true
        );

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Role must be RECEPTION, DOCTOR, or PATIENT");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(User.builder()
                .email("doctor@example.com")
                .fullName("Existing Doctor")
                .phone("555-0102")
                .passwordHash("hash")
                .role("DOCTOR")
                .build()));

        UserRequest request = new UserRequest(
                "New Doctor",
                "doctor@example.com",
                "555-0103",
                "secret123",
                "DOCTOR",
                null,
                null,
                null,
                null,
                true
        );

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email is already in use");
    }

    @Test
    void createPatientDoesNotRequireEmailOrPasswordInRequest() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRequest request = new UserRequest(
                "Walk In Patient",
                null,
                "555-0105",
                null,
                "PATIENT",
                34,
                "female",
                "Needs follow-up",
                "123 Main Street",
                true
        );

        UserService.UserResponse response = userService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getRole()).isEqualTo("PATIENT");
        assertThat(saved.getEmail()).endsWith("@internal.dentalclinic.local");
        assertThat(response.email()).isNull();
        assertThat(saved.getPasswordHash()).isNotBlank();
        assertThat(saved.getAge()).isEqualTo(34);
        assertThat(saved.getAddress()).isEqualTo("123 Main Street");
    }
}
