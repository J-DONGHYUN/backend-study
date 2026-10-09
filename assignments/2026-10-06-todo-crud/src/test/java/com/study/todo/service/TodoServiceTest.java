package com.study.todo.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.study.todo.domain.Todo;
import com.study.todo.dto.TodoCreateRequest;
import com.study.todo.dto.TodoResponse;
import com.study.todo.exception.TodoNotFoundException;
import com.study.todo.repository.TodoRepository;

/**
 * 단위 테스트 (Mockito) - Spring 컨텍스트를 띄우지 않는다.
 * Repository는 가짜(Mock)로 대체하고, TodoService의 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

	@Mock
	TodoRepository todoRepository;

	@InjectMocks
	TodoService todoService;

	@Test
	@DisplayName("등록하면 저장 후 미완료 상태로 응답한다")
	void create_성공() {
		// given
		TodoCreateRequest request = new TodoCreateRequest("운동하기", "헬스장 가기");
		given(todoRepository.save(any(Todo.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		TodoResponse response = todoService.create(request);

		// then
		assertThat(response.title()).isEqualTo("운동하기");
		assertThat(response.completed()).isFalse();
		then(todoRepository).should().save(any(Todo.class));
	}

	@Test
	@DisplayName("존재하지 않는 id로 조회하면 TodoNotFoundException이 발생한다")
	void findById_없으면_예외() {
		// given
		given(todoRepository.findById(999L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> todoService.findById(999L))
			.isInstanceOf(TodoNotFoundException.class)
			.hasMessageContaining("999");
	}

	@Test
	@DisplayName("completed가 null이면 전체 조회를 호출한다")
	void findAll_필터없음() {
		// given
		given(todoRepository.findAll())
			.willReturn(List.of(Todo.create("A", null), Todo.create("B", null)));

		// when
		List<TodoResponse> responses = todoService.findAll(null);

		// then
		assertThat(responses).hasSize(2);
		then(todoRepository).should().findAll();
		then(todoRepository).should(never()).findByCompleted(anyBoolean());
	}

	@Test
	@DisplayName("completed가 주어지면 필터링 조회를 호출한다")
	void findAll_필터있음() {
		// given
		given(todoRepository.findByCompleted(false))
			.willReturn(List.of(Todo.create("A", null)));

		// when
		List<TodoResponse> responses = todoService.findAll(false);

		// then
		assertThat(responses).hasSize(1);
		then(todoRepository).should().findByCompleted(false);
		then(todoRepository).should(never()).findAll();
	}
}
