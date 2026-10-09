package com.study.todo.api;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.study.todo.repository.TodoRepository;

import com.study.todo.dto.ErrorResponse;
import com.study.todo.dto.TodoCreateRequest;
import com.study.todo.dto.TodoResponse;

/**
 * E2E 테스트 - 실제 포트에 톰캣을 띄우고 진짜 HTTP 요청을 보낸다.
 * 클라이언트가 보는 것과 동일하다. 상태 코드, JSON 직렬화, 예외 핸들러까지 전부 검증된다.
 *
 * 주의: 진짜 HTTP라 테스트 스레드와 서버 스레드가 다르다 -> @Transactional 롤백이 안 먹는다.
 *       그래서 각 테스트는 자기 데이터를 스스로 만들어 독립적으로 굴러가야 한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TodoApiE2ETest {

	@Autowired
	TestRestTemplate restTemplate;

	@Autowired
	TodoRepository todoRepository;

	/**
	 * E2E는 실제 HTTP라 테스트 스레드와 서버 스레드가 다르다.
	 * -> @Transactional 롤백이 통하지 않고, 데이터가 DB에 그대로 커밋된다.
	 * -> 치우지 않으면 같은 H2(jdbc:h2:mem:tododb)를 쓰는 다른 테스트가 오염된다.
	 */
	@AfterEach
	void tearDown() {
		todoRepository.deleteAll();
	}

	@Test
	@DisplayName("POST /todos - 등록하면 201과 생성된 Todo를 반환한다")
	void 등록_201() {
		// given
		TodoCreateRequest request = new TodoCreateRequest("운동하기", "헬스장 가기");

		// when
		ResponseEntity<TodoResponse> response =
			restTemplate.postForEntity("/todos", request, TodoResponse.class);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().title()).isEqualTo("운동하기");
		assertThat(response.getBody().completed()).isFalse();
	}

	@Test
	@DisplayName("GET /todos/{id} - 없는 id면 404와 약속한 에러 형식을 반환한다")
	void 없는id_404() {
		// when
		ResponseEntity<ErrorResponse> response =
			restTemplate.getForEntity("/todos/999999", ErrorResponse.class);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo(404);
		assertThat(response.getBody().message()).contains("999999");
		assertThat(response.getBody().timestamp()).isNotNull();
	}

	@Test
	@DisplayName("POST /todos - 제목이 공백이면 400과 검증 메시지를 반환한다")
	void 공백제목_400() {
		// given
		TodoCreateRequest request = new TodoCreateRequest("   ", "설명");

		// when
		ResponseEntity<ErrorResponse> response =
			restTemplate.postForEntity("/todos", request, ErrorResponse.class);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().status()).isEqualTo(400);
		assertThat(response.getBody().message()).contains("title");
	}

	@Test
	@DisplayName("PATCH /todos/{id}/complete - 이미 완료된 Todo를 또 완료하면 409를 반환한다")
	void 중복완료_409() {
		// given
		Long id = restTemplate
			.postForEntity("/todos", new TodoCreateRequest("완료 테스트", null), TodoResponse.class)
			.getBody().id();
		restTemplate.patchForObject("/todos/" + id + "/complete", null, TodoResponse.class);

		// when
		ResponseEntity<ErrorResponse> response = restTemplate.exchange(
			"/todos/" + id + "/complete",
			org.springframework.http.HttpMethod.PATCH,
			null,
			ErrorResponse.class);

		// then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().status()).isEqualTo(409);
	}

	@Test
	@DisplayName("전체 흐름 - 등록하고 조회하고 완료하고 삭제한다")
	void 전체흐름() {
		// given - 등록
		Long id = restTemplate
			.postForEntity("/todos", new TodoCreateRequest("전체흐름", "설명"), TodoResponse.class)
			.getBody().id();

		// when & then - 조회
		ResponseEntity<TodoResponse> found =
			restTemplate.getForEntity("/todos/" + id, TodoResponse.class);
		assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(found.getBody().completed()).isFalse();

		// when & then - 완료 처리
		TodoResponse completed =
			restTemplate.patchForObject("/todos/" + id + "/complete", null, TodoResponse.class);
		assertThat(completed.completed()).isTrue();

		// when & then - 삭제
		ResponseEntity<Void> deleted = restTemplate.exchange(
			"/todos/" + id,
			org.springframework.http.HttpMethod.DELETE,
			null,
			Void.class);
		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		// when & then - 삭제 후 조회하면 404
		ResponseEntity<ErrorResponse> afterDelete =
			restTemplate.getForEntity("/todos/" + id, ErrorResponse.class);
		assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
