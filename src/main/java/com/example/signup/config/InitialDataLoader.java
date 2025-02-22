package com.example.signup.config;

import com.example.signup.entity.BoardCategory;
import com.example.signup.repository.BoardCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitialDataLoader {

    @Bean
    public CommandLineRunner initData(BoardCategoryRepository boardCategoryRepository) {
        return args -> {
            // 게시판 카테고리 초기 데이터 생성
            if (boardCategoryRepository.count() == 0) {
                boardCategoryRepository.save(new BoardCategory("전체", "모든 게시글을 볼 수 있는 게시판입니다."));
                boardCategoryRepository.save(new BoardCategory("취업고민", "취업과 관련된 고민을 나누는 게시판입니다."));
                boardCategoryRepository.save(new BoardCategory("이직상담", "이직과 관련된 상담을 나누는 게시판입니다."));
                boardCategoryRepository.save(new BoardCategory("스터디", "함께 공부할 스터디원을 모집하는 게시판입니다."));
                boardCategoryRepository.save(new BoardCategory("자유게시판", "자유롭게 이야기를 나누는 게시판입니다."));
            }
        };
    }
}
