package com.example.signup.controller;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.UserEntity;
import com.example.signup.entity.enum_.Gender;
import com.example.signup.exception.UserAlreadyExistsException;
import com.example.signup.service.NaverApiService;
import com.example.signup.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final NaverApiService naverApiService;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.redirect-uri}")
    private String redirectURI;

    @GetMapping("/login")
    public String showLoginForm() {
        return "login_form";
    }

    @PostMapping("/login")
    public String loginUser(
        @RequestParam String userId, 
        @RequestParam String password, 
        HttpSession session, 
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        try {
            UserEntity user = userService.authenticate(userId, password);
            user.setPassword(null); // 보안을 위해 비밀번호 제거
            session.setAttribute("user", user);
            redirectAttributes.addFlashAttribute("successMessage", "로그인 성공!");
            return "redirect:/home";
        } catch (UserAlreadyExistsException e) {
            model.addAttribute("error", "로그인에 실패하셨습니다. 아이디와 비밀번호를 확인해주세요.");
            return "login_form";
        }
    }

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("userCreateForm", new UserCreateForm());
        return "signup_form";
    }

    @PostMapping("/signup")
    public String signup(@Valid UserCreateForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "signup_form";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "passwordInCorrect", "비밀번호가 일치하지 않습니다.");
            return "signup_form";
        }

        try {
            userService.registerNewUser(form);
            return "redirect:/login";
        } catch (UserAlreadyExistsException e) {
            bindingResult.rejectValue("userId", "userIdDuplicate", e.getMessage());
            return "signup_form";
        }
    }

    @GetMapping("/login/naver")
    public String loginWithNaver() {
        String authorizationUrl = naverApiService.generateAuthorizationUrl();
        return "redirect:" + authorizationUrl;
    }

    @GetMapping("/naver/callback")
    public String naverCallback(
        @RequestParam String code, 
        @RequestParam String state, 
        Model model
    ) {
        try {
            UserEntity naverUser = naverApiService.getUserInfo(code, state);
            
            if (userService.existsByUserId(naverUser.getUserId())) {
                model.addAttribute("error", "이미 존재하는 아이디입니다. 다른 아이디로 가입해주세요.");
                return "signup_form";
            }
            
            userService.saveNaverUser(naverUser);
            
            return "redirect:/home";
        } catch (Exception e) {
            logger.error("Naver login error", e);
            model.addAttribute("error", "네이버 로그인 중 오류가 발생했습니다: " + e.getMessage());
            return "login_form";
        }
    }

    @GetMapping("/home")
    public String home(Model model, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}