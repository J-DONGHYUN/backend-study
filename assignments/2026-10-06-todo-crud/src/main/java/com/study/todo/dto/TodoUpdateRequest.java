package com.study.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TodoUpdateRequest(
	@NotBlank(message = "제목은 필수")
	@Size(max = 50, message = "제목은 50자 이하여야합니다.")
	String title,
	String description
) {
}
