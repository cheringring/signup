package com.example.signup.entity;

import com.example.signup.entity.enum_.Gender;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "user_t")
public class UserEntity {


   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long User_idx;

    @Column(nullable = false, unique = true)
    private String userId;


    @Column(nullable = false)
    private String userName;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String addr;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String occupation;

    private String interest;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
