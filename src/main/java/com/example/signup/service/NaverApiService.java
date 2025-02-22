package com.example.signup.service;

import com.example.signup.entity.UserEntity;
import com.example.signup.entity.enum_.Gender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
public class NaverApiService {

    private static final Logger logger = LoggerFactory.getLogger(NaverApiService.class);

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    @Value("${naver.api.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public NaverApiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        return String.format("https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
            URLEncoder.encode(clientId, StandardCharsets.UTF_8),
            URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
            URLEncoder.encode(state, StandardCharsets.UTF_8));
    }

    public UserEntity getUserInfo(String code, String state) throws Exception {
        // Access Token 얻기
        String tokenUrl = String.format("https://nid.naver.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&state=%s",
            clientId, clientSecret, code, state);

        ResponseEntity<String> tokenResponse = restTemplate.postForEntity(tokenUrl, null, String.class);
        Map<String, Object> tokenMap = objectMapper.readValue(tokenResponse.getBody(), Map.class);
        String accessToken = (String) tokenMap.get("access_token");

        // 사용자 정보 얻기
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        
        ResponseEntity<String> userInfoResponse = restTemplate.exchange(
            "https://openapi.naver.com/v1/nid/me",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        Map<String, Object> userInfoMap = objectMapper.readValue(userInfoResponse.getBody(), Map.class);
        Map<String, Object> response = (Map<String, Object>) userInfoMap.get("response");

        return UserEntity.builder()
            .userId((String) response.get("id"))
            .userName((String) response.get("name"))
            .email((String) response.get("email"))
            .password(UUID.randomUUID().toString()) // 임시 비밀번호
            .build();
    }
}