package com.example.signup.entity;

import com.example.signup.entity.enum_.Gender;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "User_t")
public class UserEntity {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long User_idx;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false,unique = true)
    private Long User_ID;


    @Column(nullable = false)
    private String UserName;


    @Column(nullable = false,unique = true)
    private String Email;


    @Column(nullable = false,unique = true)
    private String password;
    private String confirmPassword;


    private Date Reg_Date;


    @Enumerated(value = EnumType.STRING)
    private Gender gender;



    @Column(nullable = false)
    private String addr;

    @Column(nullable = false)
    private String Occupation;

    @Column(nullable = false)
    private String Interest;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;



}

