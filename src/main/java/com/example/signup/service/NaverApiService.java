package com.example.signup.service;

import com.example.signup.entity.UserEntity;
import com.example.signup.entity.enum_.Gender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NaverApiService {
    private static final Logger logger = LoggerFactory.getLogger(NaverApiService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    @Value("${naver.api.redirect-uri}")
    private String redirectUri;

    @Autowired
    public NaverApiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        logger.info("NaverApiService initialized with dependencies");
    }

    public String generateAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        
        // URL 인코딩
        String encodedRedirectUri = UriUtils.encodeQueryParam(redirectUri, StandardCharsets.UTF_8);
        String encodedState = UriUtils.encodeQueryParam(state, StandardCharsets.UTF_8);
        
        String authUrl = String.format("https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
            clientId,
            encodedRedirectUri,
            encodedState);
        
        logger.info("===== Naver Authorization URL Generation =====");
        logger.info("Client ID: {}", clientId);
        logger.info("Client Secret: {}", clientSecret != null ? "exists" : "null");
        logger.info("Redirect URI (original): {}", redirectUri);
        logger.info("Redirect URI (encoded): {}", encodedRedirectUri);
        logger.info("State: {}", state);
        logger.info("Generated URL: {}", authUrl);
        logger.info("===========================================");
        
        return authUrl;
    }

    public UserEntity getUserInfo(String code, String state) throws Exception {
        logger.info("===== Getting User Info from Naver =====");
        logger.info("Code: {}", code);
        logger.info("State: {}", state);
        
        // Access Token 얻기
        String tokenUrl = String.format("https://nid.naver.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&state=%s",
            clientId, clientSecret, code, state);
            
        logger.info("Token URL: {}", tokenUrl);
        
        ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenUrl, String.class);
        logger.info("Token Response Status: {}", tokenResponse.getStatusCode());
        logger.info("Token Response Body: {}", tokenResponse.getBody());

        JsonNode tokenNode = objectMapper.readTree(tokenResponse.getBody());
        String accessToken = tokenNode.get("access_token").asText();
        
        // 사용자 정보 얻기
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> userResponse = restTemplate.exchange(
            "https://openapi.naver.com/v1/nid/me",
            HttpMethod.GET,
            entity,
            String.class
        );
        
        logger.info("User Info Response Status: {}", userResponse.getStatusCode());
        logger.info("User Info Response Body: {}", userResponse.getBody());

        JsonNode userNode = objectMapper.readTree(userResponse.getBody());
        JsonNode response = userNode.get("response");
        
        String email = response.has("email") ? response.get("email").asText() : 
                      response.get("id").asText() + "@naver.com";
                      
        Gender gender = response.has("gender") ? 
                       response.get("gender").asText().equals("M") ? Gender.MALE : Gender.FEMALE : 
                       Gender.MALE;

        return UserEntity.builder()
            .userId(response.get("id").asText())
            .userName(response.get("name").asText())
            .email(email)
            .password(UUID.randomUUID().toString())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .addr("")
            .gender(gender)
            .occupation("")
            .interest("")
            .build();
    }
}