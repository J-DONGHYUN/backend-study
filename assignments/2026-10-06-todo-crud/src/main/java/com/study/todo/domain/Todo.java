package com.study.todo.domain;

import com.study.todo.exception.TodoAlreadyCompletedException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Todo extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String title;

	private String description;

	private boolean completed;

	protected Todo() {
	}
	private Todo(String title, String description) {
		this.title = title;
		this.description = description;
		this.completed = false;
	}
	public static Todo create(String title, String description) {
		return new Todo(title, description);
	}
	public void complete() {
		if(completed) {
			throw new TodoAlreadyCompletedException(getId());
		}
		this.completed = true;
	}
	public void update(String title, String description) {
		this.title = title;
		this.description = description;
	}
	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public boolean isCompleted() {
		return completed;
	}
}
