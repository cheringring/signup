package com.example.signup.controller;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.UserEntity;
import com.example.signup.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/login")
    public String showLoginForm() {
        return "login_form";
    }

    // 네이버 로그인 창으로 리디렉션
    @GetMapping("/login/naver")
    public String loginWithNaver() {
        String state = UUID.randomUUID().toString();

        String apiURL = "https://nid.naver.com/oauth2.0/authorize?response_type=code";
        apiURL += "&client_id=" + clientId;
        apiURL += "&redirect_uri=" + redirectURI;
        apiURL += "&state=" + state;

        return "redirect:" + apiURL;
    }

    // 네이버 로그인 콜백 처리
    @GetMapping("/login/naver/callback")
    public String naverCallback(@RequestParam String code, @RequestParam String state, HttpSession session, Model model) {
        try {
            UserEntity user = userService.fetchUserInfo(code, state);
            if (userService.isUserExists(user.getEmail())) {
                user = userService.findUserByEmail(user.getEmail());
            } else {
                userService.saveUser(user);
            }
            session.setAttribute("user", user);
            return "redirect:/home";
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

        if (userService.isUserExists(form.getEmail())) {
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

    @GetMapping("/check-user")
    public ResponseEntity<UserEntity> checkUser(@RequestParam String email) {
        UserEntity user = userService.getUserByEmail(email);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    }
