package com.example.signup.controller;

import com.example.signup.entity.UserEntity;
import com.example.signup.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RequiredArgsConstructor
@RestController
public class UserSeachController {


    private final UserService userService;


    // 모든 유저 조회


    @GetMapping("/users")
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }
}
