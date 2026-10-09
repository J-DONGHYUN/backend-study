package com.study.todo.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.study.todo.domain.Todo;
import com.study.todo.dto.TodoCreateRequest;
import com.study.todo.dto.TodoResponse;
import com.study.todo.repository.TodoRepository;

import jakarta.persistence.EntityManager;

/**
 * 통합 테스트 - Spring 컨텍스트 + 실제 H2 DB를 함께 띄운다.
 * Mock이 하나도 없다. Service -> Repository -> JPA -> DB 까지 진짜로 동작시킨다.
 *
 * @Transactional : 테스트가 끝나면 자동 롤백 -> 테스트끼리 데이터가 섞이지 않는다.
 */
@SpringBootTest
@Transactional
class TodoServiceIntegrationTest {

	@Autowired
	TodoService todoService;

	@Autowired
	TodoRepository todoRepository;

	@Autowired
	EntityManager em;

	@Test
	@DisplayName("등록하면 DB에 저장되고 생성/수정 시각이 자동으로 기록된다")
	void create_DB저장과_시각기록() {
		// given
		TodoCreateRequest request = new TodoCreateRequest("운동하기", "헬스장 가기");

		// when
		TodoResponse response = todoService.create(request);

		// then
		assertThat(response.id()).isNotNull();                  // IDENTITY 전략으로 id 채번됨
		assertThat(response.createdAt()).isNotNull();           // @PrePersist 동작 확인
		assertThat(response.updatedAt()).isNotNull();
		assertThat(todoRepository.findById(response.id())).isPresent();
	}

	@Test
	@DisplayName("완료 처리하면 save() 없이도 변경 감지로 DB에 반영된다")
	void complete_변경감지로_DB반영() {
		// given
		TodoResponse created = todoService.create(new TodoCreateRequest("운동하기", null));

		// when
		todoService.complete(created.id());
		em.flush();   // 변경 감지 -> UPDATE 쿼리를 DB로 내보낸다
		em.clear();   // 1차 캐시를 비운다 -> 다음 조회는 DB에서 새로 읽는다

		// then
		Todo found = todoRepository.findById(created.id()).orElseThrow();
		assertThat(found.isCompleted()).isTrue();
		assertThat(found.getUpdatedAt()).isAfter(found.getCreatedAt());  // @PreUpdate 동작 확인
	}

	@Test
	@DisplayName("completed 필터가 실제 쿼리로 동작한다")
	void findAll_필터링() {
		// given
		TodoResponse done = todoService.create(new TodoCreateRequest("완료할 것", null));
		todoService.create(new TodoCreateRequest("미완료1", null));
		todoService.create(new TodoCreateRequest("미완료2", null));
		todoService.complete(done.id());

		// when
		List<TodoResponse> all = todoService.findAll(null);
		List<TodoResponse> completed = todoService.findAll(true);
		List<TodoResponse> notCompleted = todoService.findAll(false);

		// then
		assertThat(all).hasSize(3);
		assertThat(completed).hasSize(1);
		assertThat(notCompleted).hasSize(2);
	}

	@Test
	@DisplayName("삭제하면 DB에서 사라진다")
	void delete_DB에서_제거() {
		// given
		TodoResponse created = todoService.create(new TodoCreateRequest("지울 것", null));

		// when
		todoService.delete(created.id());
		em.flush();
		em.clear();

		// then
		assertThat(todoRepository.findById(created.id())).isEmpty();
	}
}
