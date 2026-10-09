package com.board.post.dto;

import java.time.LocalDateTime;

import com.board.post.domain.Comment;

public record CommentResponse(
	Long id,
	String content,
	String author,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static CommentResponse from(Comment comment) {
		return new CommentResponse(
			comment.getId(),
			comment.getContent(),
			comment.getAuthor(),
			comment.getCreatedAt(),
			comment.getUpdatedAt()
		);
	}
}
