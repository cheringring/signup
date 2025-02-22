package com.example.signup.controller;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.UserEntity;
import com.example.signup.entity.enum_.Gender;
import com.example.signup.exception.UserAlreadyExistsException;
import com.example.signup.service.NaverApiService;
import com.example.signup.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        logger.info("===== Starting Naver Login Process =====");
        String authorizationUrl = naverApiService.generateAuthorizationUrl();
        logger.info("Redirecting to Naver authorization URL: {}", authorizationUrl);
        
        return "redirect:" + authorizationUrl;
    }

    @GetMapping("/naver/callback")
    public String naverCallback(
        @RequestParam String code, 
        @RequestParam String state, 
        Model model,
        HttpSession session
    ) {
        logger.info("===== Naver Login Callback =====");
        logger.info("Code: {}", code);
        logger.info("State: {}", state);
        
        try {
            UserEntity naverUser = naverApiService.getUserInfo(code, state);
            logger.info("Retrieved user info - ID: {}, Name: {}", naverUser.getUserId(), naverUser.getUserName());
            
            // 이미 가입된 사용자라면 바로 로그인 처리
            if (userService.existsByUserId(naverUser.getUserId())) {
                logger.info("Existing user found. Logging in...");
                UserEntity existingUser = userService.getUserByUserId(naverUser.getUserId());
                
                // Spring Security 인증 처리
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(existingUser.getUserId(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                                  SecurityContextHolder.getContext());
                
                session.setAttribute("user", existingUser);
                return "redirect:/naver-success";
            }
            
            // 새로운 사용자라면 추가 정보 입력 페이지로 이동
            logger.info("New user. Redirecting to signup form...");
            session.setAttribute("tempNaverUser", naverUser);
            return "redirect:/social/signup";
        } catch (Exception e) {
            logger.error("Naver login error", e);
            model.addAttribute("error", "네이버 로그인 중 오류가 발생했습니다: " + e.getMessage());
            return "login_form";
        }
    }

    @GetMapping("/naver-success")
    public String naverLoginSuccess(Model model, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "naver_login_success";
    }

    @GetMapping("/social/signup")
    public String showSocialSignupForm(Model model, HttpSession session) {
        UserEntity tempUser = (UserEntity) session.getAttribute("tempNaverUser");
        if (tempUser == null) {
            return "redirect:/login";
        }
        model.addAttribute("userForm", tempUser);
        return "social_signup";
    }

    @PostMapping("/social/signup/complete")
    public String completeSocialSignup(
        @ModelAttribute("userForm") UserEntity userForm,
        HttpSession session
    ) {
        try {
            // 임시 저장된 네이버 사용자 정보 가져오기
            UserEntity tempUser = (UserEntity) session.getAttribute("tempNaverUser");
            if (tempUser == null) {
                return "redirect:/login";
            }

            // 기존 정보 유지하면서 새로 입력받은 정보 업데이트
            tempUser.setNickname(userForm.getNickname());
            tempUser.setProvince(userForm.getProvince());
            tempUser.setCity(userForm.getCity());
            tempUser.setGender(userForm.getGender());

            // 사용자 저장
            UserEntity savedUser = userService.saveNaverUser(tempUser);
            
            // 세션 업데이트
            session.removeAttribute("tempNaverUser");
            session.setAttribute("user", savedUser);
            
            return "redirect:/home";
        } catch (Exception e) {
            return "redirect:/social/signup?error";
        }
    }

    @GetMapping("/company-info")
    public String companyInfo() {
        return "company_info";
    }

    @GetMapping("/interview")
    public String interview() {
        return "interview";
    }

    @GetMapping("/community")
    public String community() {
        return "community";
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

    @GetMapping("/api/check-userid")
    @ResponseBody
    public Map<String, Boolean> checkUserId(@RequestParam String userId) {
        boolean isAvailable = !userService.existsByUserId(userId);
        return Collections.singletonMap("available", isAvailable);
    }

    @GetMapping("/api/check-nickname")
    @ResponseBody
    public Map<String, Boolean> checkNickname(@RequestParam String nickname) {
        boolean isAvailable = !userService.existsByNickname(nickname);
        return Collections.singletonMap("available", isAvailable);
    }
}