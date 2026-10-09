package com.study.todo.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.study.todo.exception.TodoAlreadyCompletedException;

/**
 * 순수 단위 테스트 - Spring도 DB도 Mock도 쓰지 않는다.
 * Todo 클래스 하나만 검증한다. 가장 빠르고 가장 깨지지 않는 테스트.
 */
class TodoTest {

	@Test
	@DisplayName("생성하면 미완료 상태로 시작한다")
	void create_시작은_미완료() {
		// given
		String title = "운동하기";
		String description = "헬스장 가기";

		// when
		Todo todo = Todo.create(title, description);

		// then
		assertThat(todo.getTitle()).isEqualTo(title);
		assertThat(todo.getDescription()).isEqualTo(description);
		assertThat(todo.isCompleted()).isFalse();
	}

	@Test
	@DisplayName("완료 처리하면 completed가 true가 된다")
	void complete_성공() {
		// given
		Todo todo = Todo.create("운동하기", "헬스장 가기");

		// when
		todo.complete();

		// then
		assertThat(todo.isCompleted()).isTrue();
	}

	@Test
	@DisplayName("이미 완료된 Todo를 다시 완료하면 예외가 발생한다")
	void complete_중복이면_예외() {
		// given
		Todo todo = Todo.create("운동하기", "헬스장 가기");
		todo.complete();

		// when & then
		assertThatThrownBy(todo::complete)
			.isInstanceOf(TodoAlreadyCompletedException.class);
	}

	@Test
	@DisplayName("수정하면 제목과 설명이 바뀐다")
	void update_성공() {
		// given
		Todo todo = Todo.create("운동하기", "헬스장 가기");

		// when
		todo.update("공부하기", "자바 복습");

		// then
		assertThat(todo.getTitle()).isEqualTo("공부하기");
		assertThat(todo.getDescription()).isEqualTo("자바 복습");
	}
}
