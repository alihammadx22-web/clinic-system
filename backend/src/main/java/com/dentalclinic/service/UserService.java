package com.dentalclinic.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dentalclinic.model.User;
import com.dentalclinic.repository.UserRepository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional
public class UserService {

    private static final Set<String> VALID_ROLES = Set.of("RECEPTION", "DOCTOR", "PATIENT");
    private static final String INTERNAL_PATIENT_EMAIL_DOMAIN = "@internal.dentalclinic.local";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return findAll(null);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll(Authentication authentication) {
        if (hasRole(authentication, "PATIENT")) {
            User currentUser = findUser(UUID.fromString(authentication.getName()));
            List<UserResponse> doctors = userRepository.findByRoleAndActiveTrueOrderByFullNameAsc("DOCTOR").stream()
                    .map(UserResponse::from)
                    .toList();
            return java.util.stream.Stream.concat(java.util.stream.Stream.of(UserResponse.from(currentUser)), doctors.stream())
                    .toList();
        }
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return UserResponse.from(findUser(id));
    }

    public UserResponse create(UserRequest request) {
        return create(request, null);
    }

    public UserResponse create(UserRequest request, Authentication authentication) {
        String role = normalizeRole(request.role());
        requireValidRole(role);
        requireDoctorCreatesPatientOnly(authentication, role);
        if ("PATIENT".equals(role)) {
            requirePatientDemographics(request);
        } else {
            requireLoginCredentials(request.email(), request.password());
            requireUniqueEmail(request.email(), null);
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(emailForCreate(request.email(), role))
                .phone(request.phone())
                .passwordHash(passwordHashForCreate(request.password(), role))
                .role(role)
                .age(request.age())
                .gender(normalizeOptional(request.gender()))
                .notes(request.notes())
                .address(request.address())
                .active(request.active() == null || request.active())
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse register(RegisterRequest request) {
        requireUniqueEmail(request.email(), null);

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(requirePassword(request.password())))
                .role("PATIENT")
                .age(request.age())
                .gender(normalizeOptional(request.gender()))
                .notes(request.notes())
                .address(request.address())
                .active(true)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse update(UUID id, UserRequest request) {
        return update(id, request, null);
    }

    public UserResponse update(UUID id, UserRequest request, Authentication authentication) {
        User user = findUser(id);
        String role = normalizeRole(request.role());
        requireValidRole(role);
        requireCanUpdate(authentication, id, role);
        if ("PATIENT".equals(role)) {
            requirePatientDemographics(request);
            if (request.email() != null && !request.email().isBlank()) {
                requireUniqueEmail(request.email(), id);
            }
        } else {
            requireEmail(request.email());
            requireUniqueEmail(request.email(), id);
        }

        user.setFullName(request.fullName());
        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(emailOrNull(request.email()));
        } else if (!"PATIENT".equals(role)) {
            user.setEmail(emailOrNull(request.email()));
        }
        user.setPhone(request.phone());
        user.setRole(role);
        user.setAge(request.age());
        user.setGender(normalizeOptional(request.gender()));
        user.setNotes(request.notes());
        user.setAddress(request.address());
        user.setActive(request.active() == null || request.active());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    public void delete(UUID id) {
        User user = findUser(id);
        userRepository.delete(user);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
    }

    private void requireUniqueEmail(String email, UUID currentUserId) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(existing -> {
            if (currentUserId == null || !existing.getId().equals(currentUserId)) {
                throw new ResponseStatusException(CONFLICT, "Email is already in use");
            }
        });
    }

    private String normalizeRole(String role) {
        return role == null ? null : role.trim().toUpperCase();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private void requireValidRole(String role) {
        if (!VALID_ROLES.contains(role)) {
            throw new ResponseStatusException(BAD_REQUEST, "Role must be RECEPTION, DOCTOR, or PATIENT");
        }
    }

    private void requireDoctorCreatesPatientOnly(Authentication authentication, String requestedRole) {
        if (authentication == null) {
            return;
        }
        boolean doctor = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_DOCTOR"));
        boolean reception = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RECEPTION"));
        if (doctor && !reception && !"PATIENT".equals(requestedRole)) {
            throw new ResponseStatusException(BAD_REQUEST, "Doctors can create PATIENT users only");
        }
    }

    private void requireCanUpdate(Authentication authentication, UUID userId, String requestedRole) {
        if (authentication == null || hasRole(authentication, "RECEPTION")) {
            return;
        }
        if (hasRole(authentication, "PATIENT")) {
            UUID currentUserId = UUID.fromString(authentication.getName());
            if (currentUserId.equals(userId) && "PATIENT".equals(requestedRole)) {
                return;
            }
        }
        throw new ResponseStatusException(FORBIDDEN, "Not allowed to update this user");
    }

    private void requirePatientDemographics(UserRequest request) {
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "fullName is required");
        }
        if (request.phone() == null || request.phone().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "phone is required");
        }
        if (request.age() == null || request.age() < 0 || request.age() > 130) {
            throw new ResponseStatusException(BAD_REQUEST, "age is required");
        }
    }

    private void requireLoginCredentials(String email, String password) {
        requireEmail(email);
        requirePassword(password);
    }

    private void requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "email is required");
        }
    }

    private String emailOrNull(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    private String emailForCreate(String email, String role) {
        String normalized = emailOrNull(email);
        if (normalized != null) {
            return normalized;
        }
        if ("PATIENT".equals(role)) {
            return "patient-" + UUID.randomUUID() + INTERNAL_PATIENT_EMAIL_DOMAIN;
        }
        return null;
    }

    private String passwordHashForCreate(String password, String role) {
        if (password != null && !password.isBlank()) {
            return passwordEncoder.encode(password);
        }
        if ("PATIENT".equals(role)) {
            return passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return null;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private String requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Password is required");
        }
        return password;
    }

    public record UserRequest(
            @NotBlank @Size(max = 160) String fullName,
            @Email @Size(max = 180) String email,
            @NotBlank @Size(max = 40) String phone,
            @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 20) String role,
            Integer age,
            @Size(max = 20) String gender,
            String notes,
            String address,
            Boolean active
    ) {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(max = 40) String phone,
            @NotBlank @Size(min = 6, max = 100) String password,
            Integer age,
            @Size(max = 20) String gender,
            String notes,
            String address
    ) {
    }

    public record UserResponse(
            UUID id,
            String fullName,
            String email,
            String phone,
            String role,
            Integer age,
            String gender,
            String notes,
            String address,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static UserResponse from(User user) {
            String email = user.getEmail();
            if (email != null && email.endsWith(INTERNAL_PATIENT_EMAIL_DOMAIN)) {
                email = null;
            }
            return new UserResponse(
                    user.getId(),
                    user.getFullName(),
                    email,
                    user.getPhone(),
                    user.getRole(),
                    user.getAge(),
                    user.getGender(),
                    user.getNotes(),
                    user.getAddress(),
                    user.isActive(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
    }
}
