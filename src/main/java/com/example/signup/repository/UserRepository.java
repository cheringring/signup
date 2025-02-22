package com.example.signup.repository;

import com.example.signup.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * UserRepository
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByUserId(String userId);

    @Query("SELECT u FROM UserEntity u WHERE u.userId = :userId")
    Optional<UserEntity> findByUserId(@Param("userId") String userId);

    Optional<UserEntity> findByNickname(String nickname);
    boolean existsByNickname(String nickname);
}