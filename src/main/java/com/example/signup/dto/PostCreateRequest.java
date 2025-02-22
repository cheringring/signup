package com.example.signup.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class PostCreateRequest {
    @NotEmpty(message = "제목은 필수 항목입니다.")
    @Size(max = 200, message = "제목은 200자 이내로 작성해주세요.")
    private String title;

    @NotEmpty(message = "내용은 필수 항목입니다.")
    private String content;

    private Long categoryId;
    
    private List<String> tags = new ArrayList<>();
    
    private List<MultipartFile> images = new ArrayList<>();
}
