package com.board.global.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.board.global.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	@ExceptionHandler(value = PostNotFoundException.class)
	public ResponseEntity<ErrorResponse> handlePostNotFound(PostNotFoundException e) {
		log.warn("게시글 없음: {}", e.getMessage());
		ErrorResponse body = ErrorResponse.of(HttpStatus.NOT_FOUND, e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(value = CommentNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException e) {
		log.warn("댓글 없음: {}", e.getMessage());
		ErrorResponse body = ErrorResponse.of(HttpStatus.NOT_FOUND, e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(value = {MethodArgumentNotValidException.class,
		HttpMessageNotReadableException.class})
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.collect(Collectors.joining(", "));

		ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_REQUEST, message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(value = Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("예상치 못한 오류", e);
		ErrorResponse body = ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 오류");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
	}
}

