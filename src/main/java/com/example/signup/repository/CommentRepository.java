package com.example.signup.repository;

import com.example.signup.entity.Comment;
import com.example.signup.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPost(Post post, Pageable pageable);
    Page<Comment> findByPostAndParentIsNull(Post post, Pageable pageable);
}
