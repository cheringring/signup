package com.example.signup.entity;

import com.example.signup.entity.enum_.Gender;
import com.example.signup.entity.enum_.AuthProvider;
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

    @Column
    private String profileImage;

    @Column
    private String naverUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;  // LOCAL, NAVER, GOOGLE 등 로그인 방식

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
}
