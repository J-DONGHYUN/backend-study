package com.board.post.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.board.post.domain.Post;

public record PostResponse (
	Long id,
	String title,
	List<CommentResponse> comments,
	String content,
	String author,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
){
	public static PostResponse from(Post post) {
		return new PostResponse(
			post.getId(),
			post.getTitle(),
			post.getComments().stream()
					.map(CommentResponse::from).toList(),
			post.getContent(),
			post.getAuthor(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}
