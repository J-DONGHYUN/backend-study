package com.study.todo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.study.todo.exception.TodoNotFoundException;
import com.study.todo.domain.Todo;
import com.study.todo.dto.TodoCreateRequest;
import com.study.todo.dto.TodoResponse;
import com.study.todo.dto.TodoUpdateRequest;
import com.study.todo.repository.TodoRepository;

@Service
public class TodoService {

	private final TodoRepository todoRepository;

	public TodoService(TodoRepository todoRepository) {
		this.todoRepository = todoRepository;
	}

	@Transactional
	public TodoResponse create(TodoCreateRequest request) {
		Todo todo = Todo.create(request.title(), request.description());
		Todo saved = todoRepository.save(todo);
		return TodoResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public TodoResponse findById(Long id) {
		Todo todo = todoRepository.findById(id)
			.orElseThrow(() -> new TodoNotFoundException(id));
		return TodoResponse.from(todo);
	}

	@Transactional(readOnly = true)
	public List<TodoResponse> findAll(Boolean completed) {

		List<Todo> todos = completed == null
			? todoRepository.findAll()
			: todoRepository.findByCompleted(completed);

		return todos.stream().map(TodoResponse::from).toList();
	}

	@Transactional
	public TodoResponse update(Long id, TodoUpdateRequest request) {
		Todo todo = todoRepository.findById(id)
			.orElseThrow(() -> new TodoNotFoundException(id));

		todo.update(request.title(), request.description());
		return TodoResponse.from(todo);
	}
	@Transactional
	public TodoResponse complete(Long id) {
		Todo todo = todoRepository.findById(id)
			.orElseThrow(() -> new TodoNotFoundException(id));

		todo.complete();
		return TodoResponse.from(todo);
	}

	@Transactional
	public void delete(Long id) {
		Todo todo = todoRepository.findById(id)
			.orElseThrow(() -> new TodoNotFoundException(id));

		todoRepository.delete(todo);
	}
}
