# 2026-10-06 Todo CRUD API

## 요구사항

사내 팀원들이 할 일을 관리하는 간단한 Todo API 서버를 만든다.

### 기능

1. **Todo 등록**: 제목(title, 필수), 설명(description, 선택)을 받아 등록한다. 등록 시 완료 여부는 미완료다.
2. **Todo 단건 조회**: id로 조회한다.
3. **Todo 목록 조회**: 전체 목록을 조회한다. `completed` 쿼리 파라미터로 완료/미완료만 필터링할 수 있다. (없으면 전체)
4. **Todo 수정**: 제목과 설명을 수정한다.
5. **완료 처리**: 특정 Todo를 완료 상태로 바꾼다. 이미 완료된 Todo는 다시 완료 처리할 수 없다.
6. **Todo 삭제**: id로 삭제한다.

### 조건

- 제목은 비어 있거나 공백만 있으면 안 되고, 최대 50자다.
- 존재하지 않는 id를 조회/수정/완료/삭제하면 적절한 HTTP status와 에러 응답을 반환한다.
- 생성 시각과 수정 시각을 저장하고 응답에 포함한다.
- 에러 응답은 일관된 형식(JSON)이어야 한다. 형식은 직접 정한다.
- 서비스 계층의 핵심 로직(완료 처리 규칙 등)에 대한 테스트를 최소 3개 작성한다.

## 제약/조건

- Java, Spring Boot, Gradle, H2(인메모리)
- Controller / Service / Repository 계층을 분리한다.
- Entity를 API 응답으로 직접 반환하지 않는다.

## 진행 순서

1. 프로젝트 생성 (이 폴더에 직접 생성)
2. 아래 "내 설계"를 코드 전에 먼저 작성
3. 구현 → 막히면 힌트 요청
4. 코드 리뷰 → 회고

## 내 설계

> 처음에는 "엔티티는 title/description만", "RequestMapping으로 API 전달" 수준이었고,
> 리뷰 질문을 거치며 아래로 구체화됐다. 최종 설계만 기록한다.

### 엔티티 / 테이블
| 필드 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | `Long` | `@Id @GeneratedValue(IDENTITY)` | H2의 auto_increment로 변환됨. `long`이 아닌 이유는 저장 전 `null` 표현 |
| title | `String` | `@Column(nullable=false, length=50)` | 입력 검증(`@NotBlank`,`@Size`)은 DTO에서 별도로 |
| description | `String` | 없음 | 선택 항목이라 제약을 걸지 않음 |
| completed | `boolean` | | primitive — "모름" 상태가 없으므로 |
| createdAt / updatedAt | `LocalDateTime` | `@Column(nullable=false)` | `BaseEntity`(`@MappedSuperclass`)로 분리. `@PrePersist`/`@PreUpdate`로 자동 기록 |

- **setter를 만들지 않았다.** 상태 변경은 `complete()`, `update()`처럼 **동작 이름을 가진 메서드**로만 가능하다.
- 생성자는 2개. `protected Todo()`는 JPA 전용(DB에서 읽어올 때), 값을 받는 생성자는 `private`으로 막고 `Todo.create(...)` 정적 팩토리로만 노출.

### API
| 기능 | Method | Path | 요청 | 성공 status |
|---|---|---|---|---|
| 등록 | POST | `/todos` | body: title, description | 201 |
| 단건 조회 | GET | `/todos/{id}` | - | 200 |
| 목록 조회 | GET | `/todos` | 쿼리 `completed` (선택) | 200 |
| 수정 | PUT | `/todos/{id}` | body: title, description | 200 |
| 완료 처리 | PATCH | `/todos/{id}/complete` | - | 200 |
| 삭제 | DELETE | `/todos/{id}` | - | 204 |

- **완료 처리를 별도 엔드포인트로 분리**: 일반 수정과 적용 규칙이 다르다(이미 완료면 거부). 합치면 요청마다 "이번이 완료 요청인가"를 판별해야 한다.
- **수정은 PUT**: 구현상 title/description을 모두 받으므로 "전체 교체"에 해당한다. PATCH로 하려면 null 필드를 건너뛰는 부분 수정으로 만들어야 한다.
- **목록 필터**: `completed`가 **없으면 전체**여야 하므로 `Boolean`(nullable)로 받고, `null`이면 `findAll()`, 값이 있으면 `findByCompleted()`로 분기한다. `boolean`은 "미지정"을 표현할 수 없다.

### 계층 구조
```
controller/  TodoController          HTTP 요청/응답 변환만. 로직 없음
service/     TodoService             비즈니스 로직, 트랜잭션 경계. DTO를 받고 DTO를 반환
repository/  TodoRepository          Spring Data JPA 인터페이스
domain/      Todo, BaseEntity        엔티티 + 도메인 규칙
dto/         TodoCreateRequest       입력 검증 어노테이션
             TodoUpdateRequest
             TodoResponse            Entity -> DTO 변환(from)
             ErrorResponse
exception/   TodoNotFoundException
             TodoAlreadyCompletedException
             GlobalExceptionHandler  @RestControllerAdvice
```
- **Entity는 Service 밖으로 나가지 않는다.** Service가 DTO를 반환하므로 Controller는 Entity를 볼 수 없다.
- 의존성은 **생성자 주입 + `final`**.
- 등록/수정 요청 DTO를 분리한 이유: 지금은 내용이 같지만 요구 필드와 검증 규칙이 갈라질 때 변경 비용이 낮다.

### 예외 / 검증 정책
| 상황 | 예외 | status |
|---|---|---|
| 없는 id (조회/수정/완료/삭제) | `TodoNotFoundException` | 404 |
| 이미 완료된 Todo를 또 완료 | `TodoAlreadyCompletedException` | 409 |
| title이 공백이거나 50자 초과 | `MethodArgumentNotValidException` (Spring) | 400 |

- 커스텀 예외는 모두 **`RuntimeException` 상속(unchecked)**. ① `@Transactional`은 기본적으로 unchecked에만 롤백한다 ② 중간 계층이 `throws`/`try-catch`로 오염되지 않는다.
- Controller에 `try-catch`가 하나도 없다. `@RestControllerAdvice`가 전부 처리한다.
- 에러 응답 형식: `{"status": 404, "message": "...", "timestamp": "..."}`
- **검증은 2중으로**: 사용자 입력은 DTO의 Bean Validation, DB는 `@Column` 제약(애플리케이션을 우회하는 경로에 대한 최후 방어선).

---

## 구현 메모

막혔던 지점과 원인. **같은 실수를 반복하지 않기 위한 기록.**

| # | 막힌 지점 | 원인 / 교훈 |
|---|---|---|
| 1 | 프로젝트 안에 `demo/`가 한 겹 더 생김 | IntelliJ는 `Location/Name` 경로에 만든다. Location은 상위 폴더로 |
| 2 | **API 설계를 아예 못 했음** | 한 번도 직접 정해본 적이 없었다. URL은 리소스(명사), 동작은 HTTP method |
| 3 | 완료 처리에 **GET**을 씀 | GET은 **safe**해야 한다(상태 변경 금지). 게다가 단건 조회와 method+path가 같아져 매핑 충돌 |
| 4 | status code 400 ↔ 204 혼동 | 2xx=성공, 4xx=요청자 잘못, 5xx=서버 잘못. 앞자리로 기억 |
| 5 | 엔티티에 **title을 통째로 빠뜨림** | 요구사항을 기억에 의존해서 작성했다. **구현 후 요구사항 문서를 다시 읽고 하나씩 지울 것** |
| 6 | 생성자가 왜 필요한지 몰랐음 | setter가 없으면 생성자 말고는 값을 넣을 통로가 없다. JPA용 기본 생성자와 내 코드용 생성자는 별개 |
| 7 | `BaseEntity` 상속 후 **중복 필드를 안 지움** | Java의 field shadowing. DDL은 정상이고 테스트도 통과하지만 `getCreatedAt()`이 `null`을 반환하는 조용한 버그가 될 뻔 |
| 8 | 패키지명을 `Exception`(대문자)로 | macOS는 대소문자 구분을 안 해서 통과. **Linux/CI에서는 빌드 실패.** "내 컴퓨터에선 되는데요"의 전형 |
| 9 | `findByCompleted()` 파라미터 누락 | 메서드 이름이 `WHERE completed = ?`로 번역되는데 `?`에 넣을 값이 없었다. 앱 자체가 기동 실패 |
| 10 | `@RequestParam`에 `required=false` 누락 | 기본값이 `required=true`. `GET /todos`가 400이 나서 전체 조회가 불가능했다 |
| 11 | 중복 완료/없는 id에 **500 응답** | Spring은 모르는 예외를 전부 500으로 처리한다. 전역 예외 처리가 왜 필요한지 몸으로 확인한 지점 |
| 12 | E2E 테스트가 통합 테스트를 오염 | E2E는 실제 HTTP라 서버 스레드가 처리 → `@Transactional` 롤백이 안 통한다. `@AfterEach`로 직접 정리 |

**받은 힌트 단계:** 엔티티 구현(의사코드 단계까지), 목록 조회 stream(코드 제공), 전역 예외 처리(코드 제공), 테스트 전체(코드 제공 + 설명).

---

## 리뷰 질문과 내 답변

모의 면접 10문항의 **내 답변 원문과 보완 내용**은 같은 폴더의 `interview-log.md`에 정리했다.

| 결과 | 문항 |
|---|---|
| 🟢 정확 | 변경 감지(더티 체킹), 도메인에 규칙 두기, `@MappedSuperclass` |
| 🟡 보통 | 계층 분리 이유, 전역 예외 처리, 테스트 3종 |
| 🔴 틀림/모름 | **Entity 직접 반환 금지 이유**, **`readOnly`**, **생성자 주입**, 완료 처리 분리 이유 |

**패턴:** 직접 만들고 눈으로 확인한 것(변경 감지 → 응답의 `updatedAt`, `@MappedSuperclass` → DDL)은 정확히 설명했고, 설명만 들은 것은 흐릿했다.

---

## 회고

- 잘한 점: 정답이 아니더라도 일단 코드를 작성해보고, 아는척이라도 하면서 대답을 했던 것이 잘한 것이라 생각한다. 내가 정확히 알고 있는지 모르고 있는지도 파악할 수 있었다.
- 부족한 점: CRUD는 쉬운 건줄 알았는데 되게 복잡한 로직이었다. 개념부분에서 모르는 게 너무 많았고 기술들을 정확히 어떻게 써야하는지 몰랐다. 또한 각 클래스나, 레포지토리를 왜 나누는지 잘 몰랐다. 형식적으로 나눠야만 된다고 생각했다.
- 다음에 다르게 할 것: 우리는 RDBMS를 쓰니 관계형으로 매핑되는 것을 해보고 싶고, N+1 문제와, 실무에서 하는 방식들로 점차 코드들을 업그레이드 하고 싶다. 예를 들면 에러코드 같은 것도 공통으로 하는법 이런말이다.

## 예상 면접 질문

1. **수정할 때 `save()`를 호출하지 않았는데 어떻게 DB에 반영되나요?** 🟢
   → 조회한 엔티티는 영속 상태라 1차 캐시와 스냅샷이 함께 보관된다. 값을 바꾸면 커밋 시점에 flush가 일어나며 스냅샷과 비교(더티 체킹)해 달라진 필드만 UPDATE한다.

2. **Entity를 API 응답으로 직접 반환하지 않은 이유는?** 🔴 *(틀렸던 질문 — 반드시 준비)*
   → ① API 스펙과 DB 스키마가 묶이면 컬럼 추가가 클라이언트 모르게 응답을 바꾼다. 둘은 따로 변해야 한다. ② 민감 정보가 그대로 노출된다. (+ 지연 로딩/순환 참조 문제)

3. **완료 처리를 `PATCH /todos/{id}/complete`로 분리한 이유는?** 🔴
   → 완료는 일반 수정과 **적용되는 규칙이 다르다**(이미 완료면 거부). 엔드포인트를 합치면 모든 수정 요청마다 "이번이 완료 요청인지"를 판별해야 하고, 그 규칙이 머물 자리가 모호해진다.

### 꼬리 질문 대비
- *"데이터가 많아지면?"* → 목록 조회에 페이징(`Pageable`)이 없다. 현재는 전부 메모리에 올라온다.
- *"PUT을 쓰셨는데 PATCH와 차이는?"* → PUT은 전체 교체이고 멱등하다. 현재 구현은 title/description을 모두 받으므로 PUT이 맞다.
- *"테스트 격리는 어떻게 보장했나요?"* → 통합 테스트는 `@Transactional` 롤백, E2E는 롤백이 안 되므로 `@AfterEach`로 직접 정리.
