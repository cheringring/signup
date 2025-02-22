package com.example.signup.service;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.enum_.Gender;
import com.example.signup.repository.UserRepository;

import jakarta.transaction.Transactional;

import com.example.signup.entity.UserEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.example.signup.exception.UserAlreadyExistsException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@Service
@Transactional
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUserId(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUserId())
            .password(user.getPassword())
            .roles("USER") // 필요에 따라 역할 추가
            .build();
    }

    // 기존 인증 메서드
    public UserEntity authenticate(String userId, String password) {
        UserEntity user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new UserAlreadyExistsException("사용자를 찾을 수 없습니다."));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UserAlreadyExistsException("비밀번호가 일치하지 않습니다.");
        }
        
        return user;
    }

    // 기존 사용자 생성 메서드들
    public void createUser(UserCreateForm userCreateForm) {
        // 기존 구현 유지
    }

    // 새로 추가된 메서드들
    public boolean existsByUserId(String userId) {
        return userRepository.findByUserId(userId).isPresent();
    }

    public UserEntity getUserByUserId(String userId) {
        return userRepository.findByUserId(userId)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    public UserEntity saveNaverUser(UserEntity naverUser) {
        // 필요한 경우 추가 처리 (예: 비밀번호 암호화)
        naverUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        return userRepository.save(naverUser);
    }

    // 기존 모든 사용자 조회 메서드
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }
    
    public UserEntity registerNewUser(UserCreateForm form) {
        if (userRepository.existsByUserId(form.getUserId())) {
            throw new UserAlreadyExistsException("이미 존재하는 사용자 ID입니다.");
        }

        UserEntity newUser = UserEntity.builder()
            .userId(form.getUserId())
            .userName(form.getUserName())
            .password(passwordEncoder.encode(form.getPassword()))
            .email(form.getEmail())
            .addr(form.getAddr())
            .gender(Gender.valueOf(form.getGender().toUpperCase()))
            .occupation(form.getOccupation())
            .interest(form.getInterest())
            .createdAt(LocalDateTime.now())
            .build();

        return userRepository.save(newUser);
    }
}