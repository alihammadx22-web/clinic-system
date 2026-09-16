package com.dentalclinic.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "users_email_key", columnNames = "email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Size(max = 160)
    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Email
    @Size(max = 180)
    @Column(unique = true, length = 180)
    private String email;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String phone;

    @Size(max = 255)
    @Column(name = "password_hash")
    private String passwordHash;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String role;

    @Column
    private Integer age;

    @Size(max = 20)
    @Column(length = 20)
    private String gender;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        normalizeStrings();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        normalizeStrings();
    }

    private void normalizeStrings() {
        email = email == null || email.isBlank() ? null : email.trim().toLowerCase();
        role = role == null ? null : role.trim().toUpperCase();
        gender = gender == null || gender.isBlank() ? null : gender.trim().toUpperCase();
    }
}
