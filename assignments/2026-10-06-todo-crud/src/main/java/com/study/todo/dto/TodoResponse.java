package com.study.todo.dto;

import java.time.LocalDateTime;

import com.study.todo.domain.Todo;

public record TodoResponse (
	Long id,
	String title,
	String description,
	boolean completed,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
){
	public static TodoResponse from(Todo todo){
		return new TodoResponse(
			todo.getId(),
			todo.getTitle(),
			todo.getDescription(),
			todo.isCompleted(),
			todo.getCreatedAt(),
			todo.getUpdatedAt()
		);
	}
}
