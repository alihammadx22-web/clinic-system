package com.dentalclinic.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dentalclinic.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdAndRole(UUID id, String role);

    Optional<User> findByIdAndRole(UUID id, String role);

    List<User> findByRoleAndActiveTrueOrderByFullNameAsc(String role);
}
