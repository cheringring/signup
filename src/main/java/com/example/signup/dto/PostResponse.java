package com.example.signup.dto;

import com.example.signup.entity.Post;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private String authorName;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private List<String> imageUrls;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponse from(Post post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setAuthorName(post.getAuthor().getNickname());
        response.setViewCount(post.getViewCount());
        response.setLikeCount(post.getLikeCount());
        response.setCommentCount(post.getComments().size());
        response.setImageUrls(post.getImages().stream()
                .map(image -> "/uploads/" + image.getFileName())
                .collect(Collectors.toList()));
        response.setTags(post.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.toList()));
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        return response;
    }
}
