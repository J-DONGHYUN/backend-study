package com.board.global.exception;

public class PostNotFoundException extends RuntimeException {
	public PostNotFoundException(Long id) {
		super("게시판이 없는 id = "+id);
	}
}
