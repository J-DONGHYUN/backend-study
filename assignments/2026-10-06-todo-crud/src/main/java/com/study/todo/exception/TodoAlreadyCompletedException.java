package com.study.todo.exception;

public class TodoAlreadyCompletedException extends RuntimeException {
	public TodoAlreadyCompletedException(Long id) {

		super("이미 완료된 Todo 입니다 id = " + id);
	}
}
