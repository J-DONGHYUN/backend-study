package com.board.post.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.board.global.BaseEntity;
import com.board.global.exception.CommentNotFoundException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@BatchSize(size = 100)
	@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Comment> comments = new ArrayList<>();

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false, length = 20)
	private String author;

	private Post(String title, String content, String author) {
		this.title = title;
		this.content = content;
		this.author = author;
	}

	public static Post create(String title, String content, String author) {
		return new Post(title, content, author);
	}

	public void update(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public Comment addComment(String content, String author) {
		Comment comment = Comment.create(this, content, author);
		this.comments.add(comment);
		return comment;
	}

	public void removeComment(Long id) {
		Comment target = this.comments.stream()
			.filter(c -> c.getId().equals(id))
			.findFirst()
			.orElseThrow(() -> new CommentNotFoundException(id));
		this.comments.remove(target);
	}


}
