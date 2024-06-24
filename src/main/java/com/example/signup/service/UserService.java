package com.example.signup.service;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.repository.UserRepository;
import com.example.signup.entity.UserEntity;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NaverApiService naverApiService;

    public void createUser(UserCreateForm form) {
        UserEntity user = new UserEntity();
        user.setUserName(form.getUsername());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setAddr(form.getAddr());
        user.setGender(form.getGender());
        user.setOccupation(form.getOccupation());
        user.setInterest(form.getInterest());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    public UserEntity fetchUserInfo(String code, String state) {
        return naverApiService.getUserInfo(code, state);
    }

    // 중복 체크 메서드 추가
    public boolean isUserExists(String email, Long userId) {
        return userRepository.existsByEmail(email) || userRepository.existsByUserId(userId);
    }
}