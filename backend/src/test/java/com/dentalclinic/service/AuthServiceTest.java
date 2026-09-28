package com.dentalclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;
import com.dentalclinic.service.AuthService.LoginRequest;

class AuthServiceTest {

    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        JwtEncoder jwtEncoder = parameters -> token(parameters.getClaims());
        authService = new AuthService(userRepository, passwordEncoder, jwtEncoder, 3600);
    }

    @Test
    void loginReturnsTokenAndUserWithoutPasswordHash() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName("Reception User")
                .email("reception@example.com")
                .phone("555-0100")
                .passwordHash(passwordEncoder.encode("secret123"))
                .role("RECEPTION")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(userRepository.findByEmail("reception@example.com")).thenReturn(Optional.of(user));

        AuthService.LoginResponse response = authService.login(new LoginRequest("reception@example.com", "secret123"));

        assertThat(response.token()).isEqualTo("test.jwt.token");
        assertThat(response.user().email()).isEqualTo("reception@example.com");
        assertThat(response.user().role()).isEqualTo("RECEPTION");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName("Patient User")
                .email("patient@example.com")
                .phone("555-0101")
                .passwordHash(passwordEncoder.encode("secret123"))
                .role("PATIENT")
                .active(true)
                .build();
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("patient@example.com", "bad-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void loginRejectsPatientAccounts() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName("Patient User")
                .email("patient@example.com")
                .phone("555-0101")
                .passwordHash(passwordEncoder.encode("secret123"))
                .role("PATIENT")
                .active(true)
                .build();
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("patient@example.com", "secret123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    private Jwt token(JwtClaimsSet claims) {
        return new Jwt(
                "test.jwt.token",
                claims.getIssuedAt(),
                claims.getExpiresAt(),
                java.util.Map.of("alg", "HS256"),
                claims.getClaims()
        );
    }
}
