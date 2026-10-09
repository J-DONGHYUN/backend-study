package com.study.todo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.study.todo.dto.TodoCreateRequest;
import com.study.todo.dto.TodoResponse;
import com.study.todo.dto.TodoUpdateRequest;
import com.study.todo.service.TodoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/todos")
public class TodoController {

	private final TodoService todoService;
	public TodoController(TodoService todoService){
		this.todoService = todoService;
	}

	@PostMapping
	public ResponseEntity<TodoResponse> create(@Valid @RequestBody TodoCreateRequest request){
		TodoResponse response = todoService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	@GetMapping("/{id}")
	public ResponseEntity<TodoResponse> findById(@PathVariable Long id){
		TodoResponse response = todoService.findById(id);
		return ResponseEntity.ok(response);
	}
	@GetMapping
	public ResponseEntity<List<TodoResponse>> findAll(@RequestParam(required = false) Boolean completed){
		List<TodoResponse> responses = todoService.findAll(completed);
		return ResponseEntity.ok(responses);
	}
	@PutMapping("/{id}")
	public ResponseEntity<TodoResponse> update(@PathVariable Long id, @Valid @RequestBody TodoUpdateRequest request){
		TodoResponse response = todoService.update(id, request);
		return ResponseEntity.ok(response);
	}
	@PatchMapping("/{id}/complete")
	public ResponseEntity<TodoResponse> complete(@PathVariable Long id){
		TodoResponse response = todoService.complete(id);
		return ResponseEntity.ok(response);
	}
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id){
		todoService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
