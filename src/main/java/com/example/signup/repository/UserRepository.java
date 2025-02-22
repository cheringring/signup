package com.example.signup.repository;

import com.example.signup.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByUserId(String userId);
    Optional<UserEntity> findByUserId(String userId);
}