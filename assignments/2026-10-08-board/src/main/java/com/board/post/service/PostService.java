package com.board.post.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.board.global.dto.PageResponse;
import com.board.global.exception.PostNotFoundException;
import com.board.post.domain.Comment;
import com.board.post.domain.Post;
import com.board.post.dto.CommentCreateRequest;
import com.board.post.dto.CommentResponse;
import com.board.post.dto.PostCreateRequest;
import com.board.post.dto.PostListResponse;
import com.board.post.dto.PostResponse;
import com.board.post.dto.PostUpdateRequest;
import com.board.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
	private final PostRepository postRepository;

	@Transactional
	public PostResponse create(PostCreateRequest request) {
		Post post = Post.create(request.title(), request.content(), request.author());
		Post savedPost = postRepository.save(post);
		return PostResponse.from(savedPost);
	}

	@Transactional(readOnly = true)
	public PostResponse findById(Long id) {
		Post post = findByPostId(id);
		return PostResponse.from(post);
	}

	@Transactional(readOnly = true)
	public PageResponse<PostListResponse> findAll(Pageable pageable) {
		Page<Post> posts = postRepository.findAll(pageable);

		Page<PostListResponse> responses = posts.map(PostListResponse::from);

		return PageResponse.from(responses);
	}

	@Transactional
	public PostResponse update(Long id, PostUpdateRequest request) {
		Post post = findByPostId(id);
		post.update(request.title(), request.content());
		postRepository.flush();
		return PostResponse.from(post);
	}

	@Transactional
	public void delete(Long id) {
		Post post = findByPostId(id);
		postRepository.delete(post);
	}

	@Transactional
	public CommentResponse addComment(Long id, CommentCreateRequest request) {
		Post post = findByPostId(id);
		Comment comment = post.addComment(request.content(), request.author());
		postRepository.flush();
		return CommentResponse.from(comment);
	}

	@Transactional
	public void removeComment(Long postId, Long commentId) {
		Post post = findByPostId(postId);
		post.removeComment(commentId);
	}

	private Post findByPostId(Long id) {
		return postRepository.findById(id)
			.orElseThrow(() -> new PostNotFoundException(id));
	}

}
