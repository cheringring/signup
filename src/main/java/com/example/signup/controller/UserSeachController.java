package com.example.signup.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.signup.service.UserService;
import com.example.signup.entity.UserEntity;
import java.util.List;

@RestController
public class UserSeachController {
    private final UserService userService;

    @Autowired
    public UserSeachController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users")
    public List<UserEntity> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/api/users/search")
    public boolean checkUserIdExists(@RequestParam String userId) {
        return userService.existsByUserId(userId);
    }
}
