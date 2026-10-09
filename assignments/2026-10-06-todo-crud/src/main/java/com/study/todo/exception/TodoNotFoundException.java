package com.study.todo.exception;

public class TodoNotFoundException extends RuntimeException {
	public TodoNotFoundException(Long id) {
		super("Todo를 찾을 수 없음 id = " + id);
	}

}
