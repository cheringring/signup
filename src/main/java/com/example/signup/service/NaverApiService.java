package com.example.signup.service;


import com.example.signup.entity.UserEntity;
import com.example.signup.entity.enum_.Gender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class NaverApiService {
    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserEntity getUserInfo(String code, String state) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Access Token 요청
            String tokenUrl = "https://nid.naver.com/oauth2.0/token?grant_type=authorization_code";
            tokenUrl += "&client_id=" + clientId;
            tokenUrl += "&client_secret=" + clientSecret;
            tokenUrl += "&code=" + code;
            tokenUrl += "&state=" + state;

            ResponseEntity<String> tokenResponse = restTemplate.exchange(tokenUrl, HttpMethod.GET, null, String.class);
            Map<String, Object> tokenJson = objectMapper.readValue(tokenResponse.getBody(), Map.class);
            String accessToken = (String) tokenJson.get("access_token");

            // 사용자 정보 요청
            String apiUrl = "https://openapi.naver.com/v1/nid/me";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
            Map<String, Object> jsonResponse = objectMapper.readValue(response.getBody(), Map.class);
            Map<String, Object> responseObject = (Map<String, Object>) jsonResponse.get("response");

            UserEntity user = new UserEntity();
            user.setUserName((String) responseObject.get("nickname")); // 또는 "name"을 사용
            user.setEmail((String) responseObject.get("email"));
            String gender = (String) responseObject.get("gender");
            if (gender.equals("M")) {
                user.setGender(Gender.man);
            } else if (gender.equals("F")) {
                user.setGender(Gender.woman);
            } else {
                user.setGender(Gender.other);
            }

            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user info from Naver API", e);
        }
    }
}