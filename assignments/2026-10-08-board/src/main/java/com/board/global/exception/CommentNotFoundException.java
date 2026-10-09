package com.board.global.exception;

public class CommentNotFoundException extends RuntimeException {
	public CommentNotFoundException(Long id) {
		super("댓글이 없는 id = " + id);
	}
}
