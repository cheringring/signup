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
@Table(name = "Users")
public class UserEntity {


   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long User_idx;

    @Column(nullable = false)
    private String userId;


    @Column(nullable = false)
    private String user_name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String addr;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false)
    private String occupation;

    @Column(nullable = false)
    private String interest;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}

