package com.example.signup.entity;

import com.example.signup.entity.enum_.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long User_idx;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false)
    private String userName;  // 실제 이름

    @Column(nullable = false, unique = true)
    private String nickname;  // 닉네임

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String province;  // 도/시

    @Column(nullable = false)
    private String city;  // 시/군/구

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
