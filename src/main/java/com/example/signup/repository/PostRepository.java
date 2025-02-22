package com.example.signup.repository;

import com.example.signup.entity.BoardCategory;
import com.example.signup.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 카테고리별 게시글 조회
    Page<Post> findByCategory(BoardCategory category, Pageable pageable);
    
    // 카테고리 내 검색
    Page<Post> findByCategoryIdAndTitleContainingOrCategoryIdAndContentContaining(
            Long categoryId, String title, Long sameId, String content, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.category.id = :categoryId AND " +
           "(p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> findByCategoryIdAndTitleContainingOrCategoryIdAndContentContaining(
            @Param("categoryId") Long categoryId, 
            @Param("keyword") String keyword, 
            Pageable pageable);
    
    @Query("SELECT p FROM Post p JOIN p.author a WHERE p.category.id = :categoryId AND " +
           "a.nickname LIKE %:keyword%")
    Page<Post> findByCategoryIdAndAuthorNicknameContaining(
            @Param("categoryId") Long categoryId, 
            @Param("keyword") String keyword, 
            Pageable pageable);
    
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE p.category.id = :categoryId AND " +
           "t.name LIKE %:keyword%")
    Page<Post> findByCategoryIdAndTagsNameContaining(
            @Param("categoryId") Long categoryId, 
            @Param("keyword") String keyword, 
            Pageable pageable);
    
    // 전체 게시글 검색
    Page<Post> findByTitleContaining(String keyword, Pageable pageable);
    
    Page<Post> findByContentContaining(String keyword, Pageable pageable);
    
    Page<Post> findByAuthorNicknameContaining(String keyword, Pageable pageable);
    
    Page<Post> findByTagsNameContaining(String keyword, Pageable pageable);
    
    Page<Post> findByTitleContainingOrContentContaining(
            String titleKeyword, String contentKeyword, Pageable pageable);
}
