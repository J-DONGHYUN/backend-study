package com.board.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
	@NotBlank(message = "제목은 필수")
	@Size(max = 100, message = "제목은 100자 이하")
	String title,
	@NotBlank(message = "내용은 필수")
	String content,
	@NotBlank(message = "작성자 필수")
	@Size(max = 20, message = "작성자 이름 20자 이하")
	String author
) {
}
