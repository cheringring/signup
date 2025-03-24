package com.example.signup.controller;

import com.example.signup.Form.UserCreateForm;
import com.example.signup.entity.UserEntity;
import com.example.signup.entity.enum_.Gender;
import com.example.signup.entity.enum_.AuthProvider;
import com.example.signup.exception.UserAlreadyExistsException;
import com.example.signup.service.NaverApiService;
import com.example.signup.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.of;

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
    public String showLoginForm(@RequestParam(required = false) Boolean error, Model model) {
        if (Boolean.TRUE.equals(error)) {
            model.addAttribute("error", "아이디 또는 비밀번호가 일치하지 않습니다.");
        }
        return "login_form";
    }

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("userCreateForm", new UserCreateForm());
        return "signup_form";
    }

    @PostMapping("/signup")
    public String signup(@Valid UserCreateForm form, BindingResult bindingResult, Model model, HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "signup_form";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "passwordInCorrect", "비밀번호가 일치하지 않습니다.");
            return "signup_form";
        }

        try {
            UserEntity savedUser = userService.registerNewUser(form);
            if (savedUser != null && savedUser.getUserId() != null) {
                // 회원가입 성공 후 자동 로그인 처리
                savedUser.setPassword(null); // 보안을 위해 비밀번호 제거
                session.setAttribute("user", savedUser);
                logger.info("User saved in session after signup - ID: {}, Nickname: {}", savedUser.getUserId(), savedUser.getNickname());

                // Spring Security Context 설정
                var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                var authentication = new UsernamePasswordAuthenticationToken(savedUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                logger.info("Security context saved in session");

                return "redirect:/home";
            } else {
                model.addAttribute("error", "회원가입 처리 중 오류가 발생했습니다.");
                return "signup_form";
            }
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
            logger.info("===== Naver callback processing =====");
            logger.info("Naver callback received - code: {}, state: {}", code, state);
    
            UserEntity naverUser = naverApiService.getUserInfo(code, state);
            logger.info("Retrieved user info - ID: {}, Name: {}, Email: {}, Provider: {}", 
                    naverUser.getUserId(), naverUser.getUserName(), naverUser.getEmail(), 
                    naverUser.getProvider() != null ? naverUser.getProvider() : "null");
    
            // 데이터베이스에서 사용자 조회
            boolean userExists = userService.existsByUserId(naverUser.getUserId());
            logger.info("User exists in database: {}", userExists);
    
            if (!userExists) {
                // 새로운 사용자인 경우 회원가입 페이지로 이동
                UserEntity userForm = new UserEntity();  // 새로운 객체 생성
                userForm.setUserId(naverUser.getUserId());
                userForm.setEmail(naverUser.getEmail());
                userForm.setUserName(naverUser.getUserName());
                userForm.setProvider(AuthProvider.NAVER);
        
                session.setAttribute("tempNaverUser", naverUser);
                model.addAttribute("userForm", userForm);  // userForm 객체를 따로 생성하여 전달
                return "naver_signup_form";
            }
    
            // 기존 사용자인 경우 로그인 처리
            logger.info("Existing user found. Processing login...");
            
            // 데이터베이스에서 사용자 정보 가져오기
            UserEntity user = userService.getUserByUserId(naverUser.getUserId());
            if (user == null) {
                logger.error("User not found in database despite existsByUserId returning true");
                return "redirect:/login";
            }
            logger.info("User loaded from database - ID: {}, Nickname: {}, Provider: {}", 
                    user.getUserId(), user.getNickname(), user.getProvider());
            
            // UserDetails 객체 생성
            UserDetails userDetails = User.builder()
                .username(user.getUserId())
                .password("") // 네이버 로그인은 비밀번호가 필요없음
                .authorities("ROLE_USER")
                .build();
            
            // Spring Security Context 설정
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(user);
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.info("Security context set for user: {}", user.getUserId());
            
            // 인증 상태 확인
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            logger.info("Current authentication: {}, Principal: {}, Authenticated: {}", 
                    currentAuth != null ? currentAuth.getClass().getSimpleName() : "null",
                    currentAuth != null ? currentAuth.getPrincipal() : "null",
                    currentAuth != null ? currentAuth.isAuthenticated() : "false");
            
            // 세션에 사용자 정보 저장
            session.setAttribute("user", user);
            model.addAttribute("user", user);
            
            return "redirect:/home";
        } catch (Exception e) {
            logger.error("Error during Naver callback processing", e);
            model.addAttribute("error", "네이버 로그인 처리 중 오류가 발생했습니다.");
            return "login_form";
        }
    }

    @GetMapping("/naver-success")
    public String naverLoginSuccess(Model model, HttpSession session) {
        logger.info("===== Naver Login Success =====");
        
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication instanceof AnonymousAuthenticationToken) {
            logger.info("No authenticated user, redirecting to login");
            return "redirect:/login";
        }
        
        String userId = authentication.getName();
        logger.info("Authenticated user: {}", userId);
        
        // 실제로 DB에 사용자가 있는지 한 번 더 확인
        boolean userExists = userService.existsByUserId(userId);
        logger.info("User exists in database: {}", userExists);

        if (!userExists) {
            logger.info("User not found in database, redirecting to signup");
            SecurityContextHolder.clearContext();
            session.invalidate();
            return "redirect:/login";
        }
        
        return "redirect:/home";
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
            tempUser.setProvider(AuthProvider.NAVER);  // provider 설정

            // 사용자 저장
            UserEntity savedUser = userService.saveNaverUser(tempUser);
            logger.info("User saved successfully - ID: {}, Nickname: {}", savedUser.getUserId(), savedUser.getNickname());

            // 세션 업데이트
            session.removeAttribute("tempNaverUser");
            session.setAttribute("user", savedUser);
            logger.info("User saved in session - ID: {}, Nickname: {}", savedUser.getUserId(), savedUser.getNickname());
            
            // Spring Security 인증 처리
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(savedUser.getUserId(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            logger.info("Security context saved in session");

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
    public String home(Model model) {
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Home page accessed - Authentication: {}", authentication);
        
        if (authentication != null && authentication.isAuthenticated() && 
            !(authentication instanceof AnonymousAuthenticationToken)) {
            
            Object principal = authentication.getPrincipal();
            logger.info("Principal type: {}", principal.getClass().getName());
            
            UserEntity user;
            if (principal instanceof UserEntity) {
                // 네이버 로그인의 경우
                user = (UserEntity) principal;
                logger.info("User info from principal - ID: {}, Nickname: {}", user.getUserId(), user.getNickname());
            } else {
                // 일반 로그인의 경우
                String userId = authentication.getName();
                user = userService.getUserByUserId(userId);
                logger.info("User info from database - ID: {}, Nickname: {}", 
                    user != null ? user.getUserId() : "null", 
                    user != null ? user.getNickname() : "null");
            }
            
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("isAuthenticated", true);
            }
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
    public String showUserProfile(Model model, HttpSession session) {
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        
        String userId = authentication.getName();
        UserEntity user = userService.getUserByUserId(userId);
        model.addAttribute("user", user);
        return "userProfile";
    }

    @GetMapping("/edit-profile")
    public String showEditProfile(Model model, HttpSession session) {
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        
        String userId = authentication.getName();
        UserEntity user = userService.getUserByUserId(userId);
        model.addAttribute("user", user);
        return "editProfile";
    }

    @PostMapping("/edit-profile")
    @ResponseBody
    public ResponseEntity<?> editProfile(@RequestBody Map<String, String> updates, HttpSession session) {
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication instanceof AnonymousAuthenticationToken) {
                return ResponseEntity.status(401).body("로그인이 필요합니다.");
            }
            
            String userId = authentication.getName();
            UserEntity currentUser = userService.getUserByUserId(userId);

            // 닉네임 유효성 검사
            String nickname = updates.get("nickname");
            if (nickname == null || !nickname.matches("[가-힣a-zA-Z0-9]+")) {
                return ResponseEntity.badRequest().body("닉네임은 한글, 영문, 숫자만 입력 가능합니다.");
            }

            // 지역 유효성 검사
            String province = updates.get("province");
            String city = updates.get("city");
            if (province == null || province.isEmpty() || city == null || city.isEmpty()) {
                return ResponseEntity.badRequest().body("지역을 선택해주세요.");
            }

            // 현재 사용자의 정보를 업데이트
            currentUser.setNickname(nickname);
            currentUser.setProvince(province);
            currentUser.setCity(city);
            
            // 사용자 정보 업데이트
            UserEntity savedUser = userService.updateUser(currentUser);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "프로필이 성공적으로 업데이트되었습니다."));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("프로필 수정에 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/upload-profile-image")
    @ResponseBody
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file, HttpSession session) {
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication instanceof AnonymousAuthenticationToken) {
                return ResponseEntity.status(401).body("로그인이 필요합니다.");
            }
            
            String userId = authentication.getName();
            UserEntity user = userService.getUserByUserId(userId);

            // 파일 유효성 검사
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("파일을 선택해주세요.");
            }

            // 이미지 파일 검증
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("이미지 파일만 업로드 가능합니다.");
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

            // 기존 프로필 이미지 삭제
            if (user.getProfileImage() != null) {
                try {
                    Path oldFilePath = Paths.get("." + user.getProfileImage());
                    Files.deleteIfExists(oldFilePath);
                } catch (Exception e) {
                    // 기존 파일 삭제 실패는 무시
                }
            }

            // 사용자 프로필 이미지 경로 업데이트
            user.setProfileImage("/uploads/profile-images/" + fileName);
            userService.updateUser(user);

            return ResponseEntity.ok().body(of(
                "imageUrl", user.getProfileImage()
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body("이미지 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/delete-account")
    public String deleteAccount(HttpSession session) {
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        
        String userId = authentication.getName();
        UserEntity user = userService.getUserByUserId(userId);
        if (user != null) {
            userService.deleteUser(user.getUserId());
            session.invalidate();
        }
        return "redirect:/login";
    }
}