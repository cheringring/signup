package com.example.signup.service;

import com.example.signup.entity.*;
import com.example.signup.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {
    private final PostRepository postRepository;
    private final BoardCategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CommentRepository commentRepository;

    // 업로드된 파일이 저장될 경로
    private final String uploadDir = "uploads";

    // 게시글 작성
    public Post createPost(String title, String content, Long categoryId, 
                         List<String> tagNames, List<MultipartFile> images, 
                         UserEntity author) throws IOException {
        BoardCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다."));

        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        post.setAuthor(author);

        // 태그 처리
        if (tagNames != null) {
            for (String tagName : tagNames) {
                Tag tag = tagRepository.findByName(tagName);
                if (tag == null) {
                    tag = new Tag();
                    tag.setName(tagName);
                    tagRepository.save(tag);
                }
                post.getTags().add(tag);
            }
        }

        // 이미지 처리
        if (images != null) {
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String fileName = UUID.randomUUID().toString() + 
                                   "_" + image.getOriginalFilename();
                    
                    // 업로드 디렉토리가 없으면 생성
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }
                    
                    // 파일 저장
                    Path filePath = uploadPath.resolve(fileName);
                    Files.copy(image.getInputStream(), filePath);

                    // 이미지 정보 저장
                    PostImage postImage = new PostImage();
                    postImage.setFileName(fileName);
                    postImage.setOriginalFileName(image.getOriginalFilename());
                    postImage.setContentType(image.getContentType());
                    postImage.setFileSize(image.getSize());
                    postImage.setPost(post);
                    post.getImages().add(postImage);
                }
            }
        }

        return postRepository.save(post);
    }

    // 게시글 목록 조회
    @Transactional(readOnly = true)
    public Page<Post> getPosts(Long categoryId, Pageable pageable) {
        BoardCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다."));
        return postRepository.findByCategory(category, pageable);
    }

    // 카테고리별 게시글 검색
    public Page<Post> searchPosts(Long categoryId, String keyword, String searchType, Pageable pageable) {
        if ("title".equals(searchType)) {
            return postRepository.findByBoardCategory_IdAndTitleContaining(categoryId, keyword, pageable);
        } else if ("content".equals(searchType)) {
            return postRepository.findByBoardCategory_IdAndContentContaining(categoryId, keyword, pageable);
        } else if ("author".equals(searchType)) {
            return postRepository.findByBoardCategory_IdAndAuthor_NicknameContaining(categoryId, keyword, pageable);
        } else if ("tag".equals(searchType)) {
            return postRepository.findByBoardCategory_IdAndTags_NameContaining(categoryId, keyword, pageable);
        } else {
            // 카테고리 내 제목 + 내용 검색
            return postRepository.findByBoardCategory_IdAndTitleContainingOrBoardCategory_IdAndContentContaining(
                    categoryId, keyword, categoryId, keyword, pageable);
        }
    }

    // 전체 게시글 검색
    public Page<Post> searchAllPosts(String keyword, String searchType, Pageable pageable) {
        if ("title".equals(searchType)) {
            return postRepository.findByTitleContaining(keyword, pageable);
        } else if ("content".equals(searchType)) {
            return postRepository.findByContentContaining(keyword, pageable);
        } else if ("author".equals(searchType)) {
            return postRepository.findByAuthor_NicknameContaining(keyword, pageable);
        } else if ("tag".equals(searchType)) {
            return postRepository.findByTags_NameContaining(keyword, pageable);
        } else {
            // 전체 검색 (제목 + 내용)
            return postRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
        }
    }

    // 게시글 상세 조회
    @Transactional(readOnly = true)
    public Post getPost(Long id) {
        return postRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
    }

    // 댓글 작성
    public Comment addComment(Long postId, String content, UserEntity author, Long parentId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setAuthor(author);
        comment.setPost(post);

        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글이 존재하지 않습니다."));
            comment.setParent(parent);
        }

        return commentRepository.save(comment);
    }

    // 게시글 조회수 증가
    public void incrementViewCount(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }
}
