package com.example.signup.service;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.enum_.Gender;
import com.example.signup.repository.UserRepository;
import com.example.signup.entity.UserEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NaverApiService naverApiService;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return new org.springframework.security.core.userdetails.User(
                user.getUserId(),
                user.getPassword(),
                new ArrayList<>()
        );
    }

    public void createUser(UserCreateForm form) {
        UserEntity user = new UserEntity();
        user.setUserId(form.getUserId());
        user.setUserName(form.getUserName());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setAddr(form.getAddr());
        user.setGender(Gender.valueOf(form.getGender().toUpperCase()));
        user.setOccupation(form.getOccupation());
        user.setInterest(form.getInterest());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        try {
            userRepository.save(user);
            System.out.println("User successfully saved: " + user.getEmail());
        } catch (Exception e) {
            System.out.println("Error saving user: " + e.getMessage());
            throw new RuntimeException("Error saving user: " + e.getMessage());
        }
    }

    public UserEntity fetchUserInfo(String code, String state) {
        return naverApiService.getUserInfo(code, state);
    }

    public boolean isUserExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUserIdExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

    public UserEntity findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void saveUser(UserEntity user) {
        user.setUpdatedAt(LocalDateTime.now());
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    public UserEntity authenticate(String userId, String password) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (passwordEncoder.matches(password, user.getPassword())) {
            return user;
        } else {
            throw new RuntimeException("Invalid credentials");
        }

    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }
}