package com.board.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest (
	@NotBlank(message = "내용 필수")
	@Size(max = 500, message = "내용 500자 이하")
	String content,
	@NotBlank(message = "작성자 필수")
	@Size(max = 20, message = "작성자 이름 20자 이하")
	String author
){
}
