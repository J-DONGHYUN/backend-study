package com.board.post.dto;

import java.time.LocalDateTime;

import com.board.post.domain.Post;

public record PostListResponse(
	Long id,
	String title,
	String author,
	int commentCount,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static PostListResponse from(Post post) {
		return new PostListResponse(
			post.getId(),
			post.getTitle(),
			post.getAuthor(),
			post.getComments().size(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}
