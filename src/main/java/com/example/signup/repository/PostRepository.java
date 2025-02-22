package com.example.signup.repository;

import com.example.signup.entity.BoardCategory;
import com.example.signup.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCategory(BoardCategory category, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.category = :category AND " +
           "(p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> findByTitleOrContent(@Param("category") BoardCategory category, 
                                   @Param("keyword") String keyword, 
                                   Pageable pageable);
    
    @Query("SELECT p FROM Post p JOIN p.author a WHERE p.category = :category AND " +
           "a.nickname LIKE %:keyword%")
    Page<Post> findByAuthorName(@Param("category") BoardCategory category, 
                               @Param("keyword") String keyword, 
                               Pageable pageable);
    
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE p.category = :category AND " +
           "t.name LIKE %:keyword%")
    Page<Post> findByTagName(@Param("category") BoardCategory category, 
                            @Param("keyword") String keyword, 
                            Pageable pageable);
    
    // 카테고리별 게시글 조회
    Page<Post> findByBoardCategory_Id(Long categoryId, Pageable pageable);
    
    // 제목 검색
    Page<Post> findByTitleContaining(String keyword, Pageable pageable);
    
    // 내용 검색
    Page<Post> findByContentContaining(String keyword, Pageable pageable);
    
    // 작성자 검색
    Page<Post> findByAuthor_NicknameContaining(String keyword, Pageable pageable);
    
    // 태그 검색
    Page<Post> findByTags_NameContaining(String keyword, Pageable pageable);
    
    // 제목 또는 내용 검색 (통합 검색)
    Page<Post> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword, Pageable pageable);
    
    // 카테고리 내 제목 검색
    Page<Post> findByBoardCategory_IdAndTitleContaining(Long categoryId, String keyword, Pageable pageable);
    
    // 카테고리 내 내용 검색
    Page<Post> findByBoardCategory_IdAndContentContaining(Long categoryId, String keyword, Pageable pageable);
    
    // 카테고리 내 작성자 검색
    Page<Post> findByBoardCategory_IdAndAuthor_NicknameContaining(Long categoryId, String keyword, Pageable pageable);
    
    // 카테고리 내 태그 검색
    Page<Post> findByBoardCategory_IdAndTags_NameContaining(Long categoryId, String keyword, Pageable pageable);
    
    // 카테고리 내 제목 또는 내용 검색
    Page<Post> findByBoardCategory_IdAndTitleContainingOrBoardCategory_IdAndContentContaining(
            Long categoryId1, String titleKeyword, Long categoryId2, String contentKeyword, Pageable pageable);
}
