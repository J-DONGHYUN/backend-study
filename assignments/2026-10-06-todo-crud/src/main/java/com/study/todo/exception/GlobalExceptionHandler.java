package com.study.todo.exception;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.study.todo.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(TodoNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(TodoNotFoundException e) {
		ErrorResponse body = ErrorResponse.of(HttpStatus.NOT_FOUND, e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	@ExceptionHandler(TodoAlreadyCompletedException.class)
	public ResponseEntity<ErrorResponse> handleAlreadyCompleted(TodoAlreadyCompletedException e) {
		ErrorResponse body = ErrorResponse.of(HttpStatus.CONFLICT, e.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.collect(Collectors.joining(", "));

		ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_REQUEST, message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);	}
}
