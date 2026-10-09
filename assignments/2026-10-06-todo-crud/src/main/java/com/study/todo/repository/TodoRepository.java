package com.study.todo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.study.todo.domain.Todo;

public interface TodoRepository extends JpaRepository<Todo, Long> {
	List<Todo> findByCompleted(boolean completed);
}
