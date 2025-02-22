package com.example.signup.controller;

import com.example.signup.entity.Post;
import com.example.signup.entity.UserEntity;
import com.example.signup.dto.PostCreateRequest;
import com.example.signup.dto.PostResponse;
import com.example.signup.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService boardService;

    // 게시판 목록
    @GetMapping
    public String board(@RequestParam(required = false) Long categoryId,
                       @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        Page<Post> posts;
        String viewName;
        
        if (categoryId == null) {
            posts = boardService.getPosts(1L, pageable); // 전체 게시판
            viewName = "board/list";
        } else {
            posts = boardService.getPosts(categoryId, pageable);
            switch (categoryId.intValue()) {
                case 2:
                    viewName = "board/job-concerns";
                    break;
                case 3:
                    viewName = "board/career-advice";
                    break;
                case 4:
                    viewName = "board/study";
                    break;
                case 5:
                    viewName = "board/free";
                    break;
                default:
                    viewName = "board/list";
            }
        }
        
        model.addAttribute("posts", posts.map(PostResponse::from));
        model.addAttribute("categoryId", categoryId);
        return viewName;
    }

    // 게시글 검색
    @GetMapping("/search")
    public String search(@RequestParam(required = false) Long categoryId,
                        @RequestParam String keyword,
                        @RequestParam String searchType,
                        @PageableDefault(size = 10) Pageable pageable,
                        Model model) {
        Page<Post> searchResults;
        String viewName;

        // 전체 검색인 경우
        if (categoryId == null) {
            searchResults = boardService.searchAllPosts(keyword, searchType, pageable);
            viewName = "board/list";
        } else {
            // 특정 카테고리 내 검색
            searchResults = boardService.searchPosts(categoryId, keyword, searchType, pageable);
            switch (categoryId.intValue()) {
                case 2:
                    viewName = "board/job-concerns";
                    break;
                case 3:
                    viewName = "board/career-advice";
                    break;
                case 4:
                    viewName = "board/study";
                    break;
                case 5:
                    viewName = "board/free";
                    break;
                default:
                    viewName = "board/list";
            }
        }

        model.addAttribute("posts", searchResults.map(PostResponse::from));
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("searchType", searchType);
        return viewName;
    }

    // 글쓰기 폼
    @GetMapping("/write")
    public String writeForm(@ModelAttribute("postForm") PostCreateRequest form) {
        return "board/write";
    }

    // 글쓰기 처리
    @PostMapping("/write")
    public String write(@Valid @ModelAttribute("postForm") PostCreateRequest form,
                       BindingResult bindingResult,
                       @AuthenticationPrincipal UserEntity user,
                       RedirectAttributes redirectAttributes) throws IOException {
        if (bindingResult.hasErrors()) {
            return "board/write";
        }

        Post post = boardService.createPost(
            form.getTitle(),
            form.getContent(),
            form.getCategoryId(),
            form.getTags(),
            form.getImages(),
            user
        );

        redirectAttributes.addFlashAttribute("message", "게시글이 작성되었습니다.");
        return "redirect:/board/view/" + post.getId();
    }

    // 게시글 상세보기
    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        Post post = boardService.getPost(id);
        boardService.incrementViewCount(id);
        
        model.addAttribute("post", PostResponse.from(post));
        return "board/view";
    }
}
