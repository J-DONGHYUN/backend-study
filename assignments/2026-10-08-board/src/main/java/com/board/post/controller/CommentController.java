package com.board.post.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.board.post.dto.CommentCreateRequest;
import com.board.post.dto.CommentResponse;
import com.board.post.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/{postId}/comments")
public class CommentController {
	private final PostService postService;

	@PostMapping
	public ResponseEntity<CommentResponse> create(@PathVariable Long postId,
		@Valid @RequestBody CommentCreateRequest request) {
		CommentResponse response = postService.addComment(postId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/{commentId}")
	public ResponseEntity<Void> delete(@PathVariable Long postId,
		@PathVariable Long commentId) {
		postService.removeComment(postId, commentId);
		return ResponseEntity.noContent().build();
	}
}
