package com.example.signup.controller;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.UserEntity;
import com.example.signup.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
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

//    private final UserService userService;
//    private final UserCreateForm userCreateForm;
//
//
//    @GetMapping("/signup")
//    public String signup(UserCreateForm userCreateForm) {
//        return "signup_form";
//    }
//
//    @PostMapping("/signup")
//    public String signup(@Valid UserCreateForm userCreateForm, BindingResult bindingResult) {
//        if (bindingResult.hasErrors()) {
//            return "signup_form";
//        }
//
//        if (!userCreateForm.getPassword().equals(userCreateForm.getConfirmPassword())) {
//            bindingResult.rejectValue("password2", "passwordInCorrect",
//                    "2개의 패스워드가 일치하지 않습니다.");
//            return "signup_form";
//        }
//
//        try {
//            userService.create(
//                    userCreateForm.getUserId(),
//                    userCreateForm.getUsername(),
//                    userCreateForm.getEmail(),
//                    userCreateForm.getPassword(),
//                    userCreateForm.getConfirmPassword(),
//                    userCreateForm.getAddr(),
//                    userCreateForm.getOccupation(),
//                    userCreateForm.getInterest());
//        } catch(DataIntegrityViolationException e) {
//            e.printStackTrace();
//            bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
//            return "signup_form";
//        } catch(Exception e) {
//            e.printStackTrace();
//            bindingResult.reject("signupFailed", e.getMessage());
//            return "signup_form";
//        }
//
//        return "redirect:/";
        }
    }