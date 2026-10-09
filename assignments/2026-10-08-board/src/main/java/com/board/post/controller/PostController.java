package com.board.post.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.board.global.dto.PageResponse;
import com.board.post.dto.PostCreateRequest;
import com.board.post.dto.PostListResponse;
import com.board.post.dto.PostResponse;
import com.board.post.dto.PostUpdateRequest;
import com.board.post.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {
	private final PostService postService;

	@PostMapping
	public ResponseEntity<PostResponse> create(@Valid @RequestBody PostCreateRequest request) {
		PostResponse response = postService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{postId}")
	public ResponseEntity<PostResponse> get(@PathVariable Long postId) {
		PostResponse response = postService.findById(postId);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@GetMapping
	public ResponseEntity<PageResponse<PostListResponse>> findAll(
		@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable
	) {
		PageResponse<PostListResponse> response = postService.findAll(pageable);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PutMapping("/{postId}")
	public ResponseEntity<PostResponse> update(@PathVariable Long postId,
		@Valid @RequestBody PostUpdateRequest request) {
		PostResponse response = postService.update(postId, request);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@DeleteMapping("/{postId}")
	public ResponseEntity<Void> delete(@PathVariable Long postId) {
		postService.delete(postId);
		return ResponseEntity.noContent().build();
	}
}
