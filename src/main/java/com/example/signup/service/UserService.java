package com.example.signup.service;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.enum_.Gender;
import com.example.signup.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.signup.entity.UserEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.signup.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private UserEntity findUserOrThrow(String userId) {
        logger.debug("Attempting to find user: {}", userId);
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", userId);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
                });
    }
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserEntity userEntity = findUserOrThrow(userId);
        return User.builder()
                .username(userEntity.getUserId())
                .password(userEntity.getPassword())
                .roles("USER")
                .build();
    }
    // 기존 인증 메서드
    public UserEntity authenticate(String userId, String password) {
        UserEntity user = findUserOrThrow(userId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.error("Password mismatch for user: {}", userId);
            throw new UserAlreadyExistsException("비밀번호가 일치하지 않습니다.");
        }
        return user;
    }
    public UserEntity getUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        logger.info("User loaded: {}", userId);
        return userEntity;  // UserEntity 객체를 반환
    }

    public Optional<UserEntity> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
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

        if (userRepository.existsByNickname(form.getNickname())) {
            throw new UserAlreadyExistsException("이미 존재하는 닉네임입니다.");
        }

        UserEntity newUser = UserEntity.builder()
                .userId(form.getUserId())
                .userName(form.getUserName())
                .nickname(form.getNickname())
                .password(passwordEncoder.encode(form.getPassword()))
                .email(form.getEmail())
                .province(form.getProvince())
                .city(form.getCity())
                .gender(Gender.valueOf(form.getGender().toUpperCase()))
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(newUser);
    }

    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    public boolean existsByUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        boolean exists = userRepository.findByUserId(userId).isPresent();
        logger.info("Checking if user exists - userId: {}, exists: {}", userId, exists);
        return exists;
    }

    public void deleteUser(String userId) {
        UserEntity user = findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    public void deleteUserByIdx(Long userIdx) {
        userRepository.findById(userIdx)
                .ifPresent(user -> userRepository.delete(user));
    }

    public UserEntity updateUser(UserEntity user) {
        if (user.getUser_idx() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // 기존 사용자가 존재하는지 확인
        UserEntity existingUser = userRepository.findById(user.getUser_idx())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + user.getUser_idx()));

        // 업데이트 시간 설정
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
    public void updateProfileImage(String userId, String imageUrl) {
        UserEntity user = findUserOrThrow(userId);
        user.setProfileImage(imageUrl);
        logger.info("Updating profile image for user: {}", userId);
        userRepository.save(user);
    }
}