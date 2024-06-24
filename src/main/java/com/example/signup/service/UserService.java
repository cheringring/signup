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
    private final NaverApiService naverApiService; // NaverApiService 의존성 주입

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

    // Naver API를 통해 사용자 정보를 가져오는 메서드 추가
    public UserEntity fetchUserInfo(String code, String state) {
        return naverApiService.getUserInfo(code, state);

//    public void create(String userId, String username, String email, String password, String addr, String occupation, String interest, @NotEmpty(message = "취미는 필수항목입니다.") String userCreateFormInterest) {
//        UserEntity user = UserEntity.builder()
//                .User_ID(userId)
//                .Name(username)
//                .Email(email)
//                .Password(passwordEncoder.encode(password))
//                .addr(addr)
//                .Occupation(occupation)
//                .Interest(interest)
//                .build();
//        userRepository.save(user);
    }
}