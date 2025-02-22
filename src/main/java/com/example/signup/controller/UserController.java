package com.example.signup.controller;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.UserEntity;
import com.example.signup.entity.enum_.Gender;
import com.example.signup.exception.UserAlreadyExistsException;
import com.example.signup.service.NaverApiService;
import com.example.signup.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;

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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.util.StringUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;
import java.time.LocalDateTime;

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
        logger.info("Redirecting to Naver Authorization URL: {}", authorizationUrl);
        return "redirect:" + authorizationUrl;
    }

    @GetMapping("/login/naver/callback")
    public String naverCallback(@RequestParam String code, @RequestParam String state, Model model, HttpSession session) {
        try {
            logger.info("Naver callback received - code: {}, state: {}", code, state);
            
            UserEntity naverUser = naverApiService.getUserInfo(code, state);
            logger.info("Retrieved user info - ID: {}, Name: {}, Email: {}", 
                naverUser.getUserId(), naverUser.getUserName(), naverUser.getEmail());
            
            // 데이터베이스에서 사용자 조회
            boolean userExists = userService.existsByUserId(naverUser.getUserId());
            logger.info("User exists in database: {}", userExists);
            
            if (!userExists) {
                // 새로운 사용자인 경우 회원가입 페이지로 이동
                session.setAttribute("tempNaverUser", naverUser);
                return "naver_signup_form";
            }
            
            // 기존 사용자인 경우 로그인 처리
            logger.info("Existing user found. Processing login...");
            UserEntity existingUser = userService.getUserByUserId(naverUser.getUserId());
            
            // 세션에 사용자 정보 저장
            existingUser.setPassword(null); // 보안을 위해 비밀번호 제거
            session.setAttribute("user", existingUser);
            model.addAttribute("user", existingUser);  // Model에도 user 객체 추가
            
            // Spring Security Context 설정
            var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            var authentication = new UsernamePasswordAuthenticationToken(existingUser, null, authorities);
            var securityContext = new SecurityContextImpl();
            securityContext.setAuthentication(authentication);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
            SecurityContextHolder.setContext(securityContext);
            
            return "naver_login_success";
            
        } catch (Exception e) {
            logger.error("Error during Naver callback processing", e);
            model.addAttribute("error", "네이버 로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
            return "login_form";
        }
    }

    @GetMapping("/naver-success")
    public String naverLoginSuccess(Model model, HttpSession session) {
        logger.info("===== Naver Login Success =====");
        UserEntity user = (UserEntity) session.getAttribute("user");
        logger.info("User in session: {}", user != null ? user.getUserId() : "null");
        
        if (user == null) {
            logger.info("No user in session, redirecting to login");
            return "redirect:/login";
        }
        
        // 실제로 DB에 사용자가 있는지 한 번 더 확인
        boolean userExists = userService.existsByUserId(user.getUserId());
        logger.info("User exists in database: {}", userExists);
        
        if (!userExists) {
            logger.info("User not found in database, redirecting to signup");
            session.invalidate();
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
        return "naver_signup_form";
    }

    @PostMapping("/social/signup/complete")
    public String completeSocialSignup(
        @ModelAttribute("userForm") UserEntity userForm,
        HttpSession session
    ) {
        logger.info("===== Complete Social Signup =====");
        try {
            // 임시 저장된 네이버 사용자 정보 가져오기
            UserEntity tempUser = (UserEntity) session.getAttribute("tempNaverUser");
            if (tempUser == null) {
                logger.error("No temporary user found in session");
                return "redirect:/login";
            }
            
            logger.info("Temp user found - ID: {}, Name: {}", tempUser.getUserId(), tempUser.getUserName());
            
            // 기존 정보 유지하면서 새로 입력받은 정보 업데이트
            tempUser.setNickname(userForm.getNickname());
            tempUser.setProvince(userForm.getProvince());
            tempUser.setCity(userForm.getCity());
            tempUser.setGender(userForm.getGender());
            
            // 사용자 저장
            UserEntity savedUser = userService.saveNaverUser(tempUser);
            logger.info("User saved successfully - ID: {}, Nickname: {}", savedUser.getUserId(), savedUser.getNickname());
            
            // 세션 업데이트
            session.removeAttribute("tempNaverUser");
            session.setAttribute("user", savedUser);
            
            // Spring Security 인증 처리
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(savedUser.getUserId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, 
                              SecurityContextHolder.getContext());
            
            return "redirect:/home";
        } catch (Exception e) {
            logger.error("Error during social signup completion", e);
            return "redirect:/login";
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
        if (user != null) {
            model.addAttribute("user", user);
        }
        return "home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
    @GetMapping("/naver-logout")
    public String naverLogout(HttpSession session) {
        session.removeAttribute("naverUser");
        session.removeAttribute("accessToken");
        return "redirect:/login";
    }

    @PostMapping("/delete-naver-account")
    public String deleteNaverAccount(HttpSession session) {
        UserEntity naverUser = (UserEntity) session.getAttribute("naverUser");
        if (naverUser != null) {
            userService.deleteUserByIdx(naverUser.getUser_idx());
            session.removeAttribute("naverUser");
            session.removeAttribute("accessToken");
        }
        return "redirect:/login";
    }

    @GetMapping("/api/check-userid")
    @ResponseBody
    public Map<String, Boolean> checkUserId(@RequestParam String userId) {
        boolean exists = userService.existsByUserId(userId);
        return Collections.singletonMap("exists", exists);
    }

    @GetMapping("/api/check-nickname")
    @ResponseBody
    public Map<String, Boolean> checkNickname(@RequestParam String nickname) {
        boolean exists = userService.existsByNickname(nickname);
        return Collections.singletonMap("exists", exists);
    }

    @GetMapping("/profile")
    public String showProfile(Model model, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "userProfile";
    }

    @GetMapping("/edit-profile")
    public String showEditProfile(Model model, HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "editProfile";
    }

    @PostMapping("/edit-profile")
    public String updateProfile(@ModelAttribute UserEntity userForm, HttpSession session) {
        UserEntity currentUser = (UserEntity) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        currentUser.setNickname(userForm.getNickname());
        currentUser.setProvince(userForm.getProvince());
        currentUser.setCity(userForm.getCity());
        currentUser.setGender(userForm.getGender());

        UserEntity updatedUser = userService.updateUser(currentUser);
        session.setAttribute("user", updatedUser);

        return "redirect:/profile";
    }

    @PostMapping("/edit-profile")
    @ResponseBody
    public ResponseEntity<?> editProfile(@ModelAttribute UserEntity updatedUser, HttpSession session) {
        try {
            UserEntity currentUser = (UserEntity) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(401).body("User not logged in");
            }

            // 현재 사용자의 ID를 설정
            updatedUser.setUser_idx(currentUser.getUser_idx());
            updatedUser.setUserId(currentUser.getUserId());
            
            // 비밀번호는 현재 사용자의 것을 유지
            updatedUser.setPassword(currentUser.getPassword());
            
            // 프로필 이미지는 현재 설정된 것을 유지
            updatedUser.setProfileImage(currentUser.getProfileImage());
            
            // 사용자 정보 업데이트
            UserEntity savedUser = userService.updateUser(updatedUser);
            
            // 세션 업데이트
            savedUser.setPassword(null); // 보안을 위해 비밀번호 제거
            session.setAttribute("user", savedUser);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to update profile: " + e.getMessage());
        }
    }

    @PostMapping("/upload-profile-image")
    @ResponseBody
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file, HttpSession session) {
        try {
            UserEntity user = (UserEntity) session.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(401).body("User not logged in");
            }

            // 파일 저장 경로 설정
            String uploadDir = "./uploads/profile-images/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 파일 이름 생성 (UUID 사용)
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 사용자 프로필 이미지 경로 업데이트
            user.setProfileImage("/uploads/profile-images/" + fileName);
            userService.updateUser(user);

            return ResponseEntity.ok().body(Map.of(
                "imageUrl", user.getProfileImage()
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to upload image: " + e.getMessage());
        }
    }

    @PostMapping("/delete-account")
    public String deleteAccount(HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        userService.deleteUser(user.getUserId());
        session.invalidate();
        return "redirect:/login";
    }
}