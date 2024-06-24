package com.example.signup.controller;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.UserEntity;
import com.example.signup.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RequiredArgsConstructor
@Controller
public class UserController {

    private final UserService userService;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.redirect-uri}")
    private String redirectURI;

    @GetMapping("/login/naver")
    public String loginWithNaver() {
        String state = UUID.randomUUID().toString();

        String apiURL = "https://nid.naver.com/oauth2.0/authorize?response_type=code";
        apiURL += "&client_id=" + clientId;
        apiURL += "&redirect_uri=" + redirectURI;
        apiURL += "&state=" + state;

        return "redirect:" + apiURL;
    }

    @GetMapping("/login/naver/callback")
    public String naverCallback(@RequestParam String code, @RequestParam String state, Model model) {
        try {
            UserEntity user = userService.fetchUserInfo(code, state);
            model.addAttribute("user", user);
            return "userProfile";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("userCreateForm", new UserCreateForm());
        return "signup_form";
    }
    @PostMapping("/signup")
    public String signupUser(@Valid UserCreateForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "signup_form";
        }

        // 중복 체크
        if (userService.isUserExists(form.getEmail(), form.getUserId())) {
            model.addAttribute("error", "이미 등록된 사용자입니다.");
            return "signup_form";
        }

        try {
            userService.createUser(form);
            model.addAttribute("message", "회원가입이 완료되었습니다.");
            return "signup_success";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "signup_form";
        }
    }
}
