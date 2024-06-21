package com.example.signup.controller;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.UserEntity;
import com.example.signup.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
        String state = UUID.randomUUID().toString(); // 상태 토큰으로 사용할 임의의 문자열 생성

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
            return "userProfile"; // 사용자 정보를 보여줄 템플릿 이름
        } catch (Exception e) {
            // 예외 처리 (로그 및 사용자에게 에러 메시지 전달 등)
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "error"; // 에러를 보여줄 템플릿 이름
        }
    }

    // 회원가입 페이지로 이동하는 메서드
    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("userCreateForm", new UserCreateForm());
        return "signup_form"; // signup_form.html 뷰를 반환
    }

    // 회원가입 폼 데이터를 처리하는 메서드
    @PostMapping("/signup")
    public String signupUser(@Valid UserCreateForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "signup_form";
        }

        try {
            userService.createUser(form);
            return "redirect:/login"; // 회원가입 성공 후 로그인 페이지로 리다이렉트
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "signup_form";
        }
    }
}