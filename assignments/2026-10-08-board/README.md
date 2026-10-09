# 2026-10-08 게시판 API — 연관관계 · 페이징 · N+1

## 요구사항

게시판 API를 만든다. **게시글(Post)** 하나에 **댓글(Comment)** 여러 개가 달린다.

### 기능

| # | 기능 | 설명 |
|---|---|---|
| 1 | 게시글 등록 | title(필수, 100자 이하), content(필수), author(필수, 20자 이하) |
| 2 | 게시글 단건 조회 | **댓글 목록을 함께** 반환 |
| 3 | 게시글 목록 조회 | **페이징**. 최신순. 각 항목에 **댓글 수(commentCount)** 포함 |
| 4 | 게시글 수정 | title, content |
| 5 | 게시글 삭제 | **달려 있던 댓글도 함께 삭제**된다 |
| 6 | 댓글 등록 | 특정 게시글에 단다. content(필수, 500자 이하), author(필수, 20자 이하) |
| 7 | 댓글 삭제 | |

### 조건

- 목록 조회는 **페이징**한다. **1단계는 offset(`Pageable`)**, 응답에 `전체 개수`·`현재 페이지`·`전체 페이지 수` 포함.
  **2단계로 cursor 방식으로 전환**하고 두 방식을 비교 기록한다. (커서에는 '전체 페이지 수' 개념이 없다)
- 없는 게시글 id로 조회/수정/삭제하면 **404**.
- **없는 게시글에 댓글을 달면 404.**
- 없는 댓글 id로 삭제하면 404.
- 에러 응답은 일관된 JSON 형식.
- 양방향으로 할지 단방향으로 할지 **직접 판단하고 아래 "내 설계"에 이유를 적는다.**
- Entity를 API 응답으로 직접 반환하지 않는다.

---

### ⭐ 이번 과제의 핵심 조건 — 쿼리 수를 직접 센다

> **각 API를 호출했을 때 실행된 SQL 쿼리 수를 세어서 아래 "쿼리 수 측정 기록"에 적는다.**

1. 게시글 **10개**, 각 게시글에 댓글 **3개**씩 넣는다. (게시글 10, 댓글 30)
2. `GET /posts?page=0&size=10` 을 호출한다.
3. **실행된 쿼리가 몇 개인지 센다.**
4. 많다면 **왜 그런지 설명하고, 해결한 뒤 전/후를 비교해서 기록**한다.

쿼리 수를 세려면 `application.yml`에 설정이 필요하다.

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        generate_statistics: true   # 세션당 실행된 쿼리 수가 로그에 찍힌다
```

> 해결 방법은 **여러 가지**가 있다. 고른 방법과 **고려했다가 버린 방법, 그 이유**까지 적을 것.

---

### Lombok 사용 규칙

의존성은 이미 추가되어 있다. **다만 제한을 둔다.**

**허용**

| 어노테이션 | 쓸 곳 | 대신해주는 것 |
|---|---|---|
| `@Getter` | Entity, DTO | getter 전부 |
| `@NoArgsConstructor(access = AccessLevel.PROTECTED)` | Entity | JPA용 기본 생성자 |
| `@RequiredArgsConstructor` | Service, Controller | `final` 필드를 받는 생성자 |
| `@Slf4j` | 로그 찍을 클래스 | Logger 선언 |

**금지 ⛔ (Entity에)**

| 어노테이션 | 이유 |
|---|---|
| `@Setter` | **도메인 규칙을 우회**할 수 있다. 상태 변경은 의도가 드러나는 메서드로만 |
| `@Data` | `@Setter` + `@EqualsAndHashCode` + `@ToString`을 전부 포함 |
| `@AllArgsConstructor` | 아무 조합이나 만들 수 있다. 정적 팩토리를 쓰는 의미가 사라진다 |
| `@ToString` | **양방향 연관관계에서 무한 순환 → StackOverflowError** |

> 리뷰 때 **"이 어노테이션이 무슨 코드를 생성하는지"** 를 묻는다. 답할 수 있어야 쓴 것이다.
> IntelliJ의 `Refactor → Delombok` 으로 생성된 코드를 직접 볼 수 있다. 한 번 해볼 것.

---

### 로깅 (SLF4J)

SLF4J는 `spring-boot-starter`에 이미 포함되어 있다. 의존성 추가 불필요.

- **전역 예외 핸들러에 로그를 남긴다. 예외 성격에 따라 레벨을 다르게 한다.**
  - 404 / 400 같은 **클라이언트 잘못** → `WARN` 이하 (혹은 남기지 않음)
  - **서버 잘못(500) → 반드시 `ERROR` + 스택트레이스**
- 처리되지 않은 예외(`Exception.class`)를 잡는 **fallback 핸들러**를 추가해 500도 일관된 JSON으로 내보낸다.
- **레벨을 그렇게 나눈 기준과 이유를 "내 설계"에 적는다.**

> 생각해볼 것: 없는 URL을 긁는 봇이 하루 수만 번 요청하면, 404를 ERROR로 남겼을 때 로그가 어떻게 되는가?

---

### 테스트

- 연관관계가 의도대로 동작하는지 (게시글 삭제 시 댓글도 삭제되는지 등)
- 페이징이 동작하는지
- **N+1이 해결됐음을 검증하는 테스트** — 실행된 쿼리 수를 단언한다

## 제약/조건

- Java, Spring Boot, Gradle, H2(인메모리)
- Controller / Service / Repository 계층을 분리한다.
- 의존성 주입은 생성자 주입.

## 진행 순서

1. 프로젝트 설정 정리 (패키지명, 의존성, `application.yml`)
2. 아래 "내 설계"를 **코드 전에** 작성
3. 구현 → 막히면 힌트 요청
4. 쿼리 수 측정 → N+1 해결 → 전/후 기록
5. 코드 리뷰 → 회고

---

## 내 설계 (구현 전에 작성)

모르면 **"모름"이라고 쓰고 힌트를 요청**할 것. 추측을 사실처럼 쓰지 않는다.

### 1. 엔티티 / 테이블

**검증은 2중으로 건다.** 역할이 다르다.
- **DTO** → `@NotBlank`, `@Size` : 사용자 입력 검증. `@Valid`가 트리거. 실패 시 400
- **Entity** → `@Column(nullable, length)` : DB 제약. 애플리케이션을 우회하는 경로에 대한 최후 방어선

**Post**

| 필드 | 타입 | Entity 제약 | DTO 검증 |
|---|---|---|---|
| id | `Long` | `@Id @GeneratedValue(IDENTITY)` | - |
| title | `String` | `@Column(nullable=false, length=100)` | `@NotBlank` `@Size(max=100)` |
| content | `String` | `@Column(nullable=false, columnDefinition="TEXT")` | `@NotBlank` |
| author | `String` | `@Column(nullable=false, length=20)` | `@NotBlank` `@Size(max=20)` |
| comments | `List<Comment>` | `@OneToMany(mappedBy="post", cascade=ALL, orphanRemoval=true)` | - |

**Comment**

| 필드 | 타입 | Entity 제약 | DTO 검증 |
|---|---|---|---|
| id | `Long` | `@Id @GeneratedValue(IDENTITY)` | - |
| content | `String` | `@Column(nullable=false, length=500)` | `@NotBlank` `@Size(max=500)` |
| author | `String` | `@Column(nullable=false, length=20)` | `@NotBlank` `@Size(max=20)` |
| post | `Post` | `@ManyToOne(fetch=LAZY) @JoinColumn(name="post_id")` | - |

- `content`가 길 수 있으므로 `length` 대신 `columnDefinition="TEXT"` 검토. (요구사항에 길이 제한 없음)
- **`BaseEntity` 재사용.** `createdAt`/`updatedAt`을 Post·Comment 둘 다 상속받는다.
  과제 #1의 `@MappedSuperclass` + `@PrePersist`/`@PreUpdate` 방식을 그대로 가져온다.
- **Entity에 setter를 만들지 않는다.** 상태 변경은 동작 이름을 가진 메서드로만.

### 2. 연관관계

**방향: 양방향 (Post ←→ Comment)**
- 하나의 게시글에 여러 댓글이 달리므로 1:N. `Post`는 `@OneToMany`, `Comment`는 `@ManyToOne`.
- 양방향으로 간 이유: **`cascade`/`orphanRemoval`은 `@OneToMany` 쪽에 선언해야 한다.**
  게시글 삭제 시 댓글도 삭제되어야 하므로, `Post`가 `comments` 컬렉션을 들고 있어야 한다.
- 단방향(`Comment → Post`)만으로도 기능 구현은 가능하지만, 삭제를 Service에서 직접 처리해야 한다.

**주인: `Comment`**
- **외래키(`post_id`)를 가진 쪽이 연관관계의 주인**이다.
- 주인이 아닌 `Post.comments`는 **읽기 전용**이다.
  `post.getComments().add(c)`만 하고 `c.setPost(post)`를 빠뜨리면 **DB에 반영되지 않는다.**
  (외래키는 `comment` 테이블에 있으므로)

**FetchType: 양쪽 다 `LAZY`**
- `@ManyToOne`의 **기본값은 `EAGER`** 이므로 **명시적으로 `LAZY`로 바꾼다.**
  (`@OneToMany`는 기본이 `LAZY`)
- `EAGER`면 `Comment`를 조회할 때마다 `Post`가 **항상** 딸려온다. 필요 없을 때도.
- `LAZY`는 연관 엔티티 자리에 **프록시(가짜 껍데기)** 를 넣어두고,
  실제로 값을 쓰는 순간에 SELECT를 날린다.
- 이번 과제의 **N+1 문제와 직결**된다.

**`cascade = CascadeType.ALL`, `orphanRemoval = true`** — `Post`의 `@OneToMany`에 선언

| | 의미 |
|---|---|
| `cascade` | **영속성 전이.** 부모에게 한 동작(저장/삭제)을 자식에게도 전파 |
| `orphanRemoval` | **고아 객체 제거.** 부모 컬렉션에서 빠진 자식을 DELETE (부모가 살아있어도) |

- **왜 써도 되는가:** 댓글은 **게시글 없이 존재할 의미가 없다.** 생명주기가 종속적이다.
- **⚠️ 판단 기준:** cascade는 **"단일 소유자 + 생명주기 종속"** 일 때만 쓴다.
  회원-주문처럼 독립적인 관계에 걸면 회원 탈퇴 시 주문 기록이 전부 날아간다.

**DB의 `ON DELETE CASCADE`와의 차이**

| | JPA `cascade` | DB `ON DELETE CASCADE` |
|---|---|---|
| 주체 | 애플리케이션(Hibernate) | DB |
| 쿼리 수 | **자식 수만큼 DELETE** | 1번 |
| 성능 | 느림 | 빠름 |
| 영속성 컨텍스트 동기화 | ✅ | ❌ (캐시와 불일치) |
| `@PreRemove` 콜백 | ✅ 동작 | ❌ 안 돎 |

→ 이번 과제는 **JPA cascade**로 간다. 둘을 동시에 걸면 꼬인다.

> **댓글 삭제(기능 7)는 `post.getComments().remove(comment)` 방식으로 구현**해서
> `orphanRemoval`이 실제로 동작하는 것을 확인한다.
> (`commentRepository.delete()`로 해도 되지만, 그러면 `orphanRemoval`이 필요 없어진다.)

### 3. API (method, path, status code)

| 기능 | Method | Path | 요청 | 성공 status |
|---|---|---|---|---|
| 게시글 등록 | POST | `/posts` | body: title, content, author | **201** |
| 게시글 단건 조회 | GET | `/posts/{postId}` | - | 200 |
| 게시글 목록 조회 | GET | `/posts` | 쿼리: 페이징 파라미터 | 200 |
| 게시글 수정 | PUT | `/posts/{postId}` | body: title, content | 200 |
| 게시글 삭제 | DELETE | `/posts/{postId}` | - | **204** |
| 댓글 등록 | POST | `/posts/{postId}/comments` | body: content, author | **201** |
| 댓글 삭제 | DELETE | `/posts/{postId}/comments/{commentId}` | - | **204** |

- **생성은 201**(새 리소스가 생김), **삭제는 204**(성공했고 돌려줄 본문 없음).
- **댓글 URL을 중첩(nested)으로 둔 이유**
  - 댓글은 게시글 없이 존재할 수 없다(생명주기 종속). **URL이 그 소속 관계를 그대로 드러낸다.**
  - `postId`가 경로에 있으므로 **"이 댓글이 정말 이 게시글의 것인가"를 검증**할 수 있다.
  - `/comments/{commentId}`로도 동작은 하지만(commentId가 전역 고유), 소속 검증을 못 한다.

### 4. 페이징 — 2단계로 진행한다

**1단계: offset 방식 (`Pageable`)** — 먼저 이걸로 N+1을 측정·해결한다.
**2단계: cursor 방식으로 전환** — 같은 기능을 다시 구현하고 쿼리를 비교한다.

> **왜 offset을 먼저 하는가:** 이번 과제의 본론은 **N+1 측정**이다.
> 커서 페이징을 처음 구현하면서 동시에 N+1을 잡으면 **변수가 둘이라 원인을 분리할 수 없다.**
> 검증된 표준(`Pageable`)으로 N+1을 먼저 해결하고, 그다음 커서로 바꾼다.

#### 두 방식의 차이

```
[Offset]  GET /posts?page=2&size=10
          SELECT * FROM post ORDER BY id DESC LIMIT 10 OFFSET 20
                                                        ↑ 앞의 20개를 읽고 버린다

[Cursor]  GET /posts?cursor=42&size=10
          SELECT * FROM post WHERE id < 42 ORDER BY id DESC LIMIT 10
                                   ↑ 인덱스로 바로 점프
```

| | Offset | Cursor |
|---|---|---|
| 깊은 페이지 성능 | **느려짐** (OFFSET 100000이면 10만 건 읽고 버림) | **일정** |
| 중간에 데이터 추가/삭제 | **중복·누락 발생** | 없음 |
| "5페이지로 점프" | ✅ | ❌ |
| 전체 페이지 수 | ✅ | ❌ **개념 자체가 없음** |
| Spring Data 지원 | `Pageable` 기본 제공 | 직접 구현 |

#### 응답 DTO 모양

```jsonc
// 1단계 — offset              // 2단계 — cursor
{                              {
  "content": [ ... ],            "content": [ ... ],
  "totalElements": 103,          "nextCursor": 32,   // 다음 요청에 쓸 값
  "page": 2,                     "hasNext": true
  "size": 10,                  }
  "totalPages": 11
}
```

> ⚠️ `Page<Entity>`를 그대로 반환하면 **Entity가 노출**된다. `Page<PostListResponse>`로 변환하거나
> 직접 만든 응답 DTO로 감쌀 것.

#### 2단계 전환 후 기록할 것
- 쿼리가 어떻게 달라졌는가 (특히 **offset 방식의 COUNT 쿼리**)
- 커서 방식에서 포기한 것은 무엇인가

### 5. 계층 구조 / 패키지

**도메인형(domain-first) 구조로 간다.** 과제 #1은 계층형(layer-first)이었다.

```
com.board
├── post/
│   ├── controller/   PostController
│   ├── service/      PostService
│   ├── repository/   PostRepository
│   ├── domain/       Post
│   └── dto/          PostCreateRequest, PostUpdateRequest,
│                     PostResponse, PostListResponse
├── comment/
│   ├── controller/   CommentController
│   ├── service/      CommentService
│   ├── repository/   CommentRepository
│   ├── domain/       Comment
│   └── dto/          CommentCreateRequest, CommentResponse
└── global/
    ├── domain/       BaseEntity
    ├── dto/          ErrorResponse
    └── exception/    GlobalExceptionHandler, 커스텀 예외들
```

| | 계층형 (과제 #1) | 도메인형 (과제 #2) |
|---|---|---|
| 장점 | 단순. 작은 프로젝트에 적합 | **도메인이 늘어도 영향 범위가 한 폴더** |
| 단점 | 도메인 늘면 각 폴더가 비대 | 초기에 폴더 수가 많음 |

- 의존성 주입은 **생성자 주입 + `final`** (Lombok `@RequiredArgsConstructor`)
- Entity는 Service 밖으로 나가지 않는다. Service가 DTO를 받고 DTO를 반환한다.

> 고민거리: Comment는 Post 없이 존재할 수 없고(cascade로 묶임, URL도 중첩) 종속적이다.
> `board/` 하나로 묶는 선택지도 있다. 지금은 분리로 가되, **"왜 분리했는가"** 를 답할 수 있어야 한다.

### 6. 예외 / 검증 정책

| 상황 | 예외 | status | 로그 레벨 |
|---|---|---|---|
| 없는 게시글 id로 조회/수정/삭제 | `PostNotFoundException` | 404 | WARN |
| **없는 게시글에 댓글 등록** | `PostNotFoundException` | 404 | WARN |
| 없는 댓글 id로 삭제 | `CommentNotFoundException` | 404 | WARN |
| **댓글이 해당 게시글 소속이 아님** | `CommentNotBelongToPostException` | 400 | WARN |
| 검증 실패 (`@Valid`) | `MethodArgumentNotValidException` | 400 | WARN |
| **그 외 처리되지 않은 예외** | `Exception` (fallback) | **500** | **ERROR + 스택트레이스** |

- 커스텀 예외는 모두 **`RuntimeException` 상속(unchecked)**.
  ① `@Transactional`은 기본적으로 unchecked에만 롤백 ② 중간 계층이 `throws`로 오염되지 않음
- Controller에 `try-catch`를 쓰지 않는다. `@RestControllerAdvice`가 전부 처리.
- 에러 응답 형식은 과제 #1과 동일: `{"status":404,"message":"...","timestamp":"..."}`
- **`CommentNotBelongToPostException`이 필요한 이유:** URL이
  `DELETE /posts/{postId}/comments/{commentId}` 이므로, 그 댓글이 **정말 그 게시글의 것인지**
  검증해야 한다. 다른 게시글의 댓글 id를 넣어도 지워지면 안 된다.

### 7. Entity에 `@Setter`를 쓰지 않는 이유

**규칙을 검증하고 변경되어야 하는데, setter가 있으면 규칙 없이 바로 수정될 수 있다.**

```java
post.setTitle("...")        // 왜 바꾸는지 의도가 안 보이고, 검증을 거치지 않는다
post.update(title, content) // 의도가 드러나고, 안에 규칙을 넣을 수 있다
```

- 공들여 만든 도메인 규칙에 **뒷문을 열어두는 셈**이다.
- `createdAt` 같은 불변이어야 할 값까지 조작 가능해진다.
- 그래서 Lombok을 쓰되 **Entity에 `@Setter`·`@Data`·`@AllArgsConstructor`는 금지**한다.

### 8. 양방향 연관관계에 `@ToString`을 붙이면 무슨 일이 생기는가

**서로를 참조하므로 무한 순환에 빠져 `StackOverflowError`가 난다.**

```
post.toString()
  └─ comments 필드도 찍자
       └─ comment1.toString()
            └─ post 필드도 찍자
                 └─ post.toString()      ← 다시 처음
                      └─ ... 무한 반복 💥 StackOverflowError
```

**같은 문제가 세 군데에서 난다.**

| 어디서 | 증상 |
|---|---|
| `@ToString` | `StackOverflowError` |
| **Jackson 직렬화** | 같은 무한 순환 → 응답 생성 중 터짐 |
| `@EqualsAndHashCode` | 같은 무한 순환 |

> **이것이 "Entity를 API 응답으로 직접 반환하면 안 되는 이유" 중 하나다.**
> DTO로 변환하면 순환을 끊을 지점이 생겨 셋 다 해결된다.

**그래서 `@Data`가 위험하다.** `@Data` = `@Getter` + `@Setter` + `@ToString`
+ `@EqualsAndHashCode` + `@RequiredArgsConstructor` → Entity에 위험한 것 3개가 한 번에 들어온다.

**대응**
- 가장 안전: Entity에 `@ToString`을 **아예 붙이지 않는다** ← 이번 과제 방침
- 꼭 필요하면 `@ToString(exclude = "post")` 로 **한쪽 방향만 끊는다**
- **로그에 엔티티를 통째로 찍지 않는다.** `log.info("postId={}", post.getId())` 처럼 필요한 값만

### 9. 로그 레벨을 나누는 기준과 이유

**판단 기준은 하나: "이 로그를 누가, 언제 보는가?"**

| 레벨 | 누가 언제 보나 | 예 |
|---|---|---|
| **ERROR** | **지금 조치가 필요하다.** 알림이 울려야 한다 | DB 연결 끊김, 예상 못 한 500 |
| **WARN** | 이상하지만 서비스는 돈다. 나중에 확인 | 재시도 후 성공, 잘못된 요청 |
| **INFO** | 운영 중 "무슨 일이 있었나" 추적 | 게시글 등록/삭제, 앱 기동 |
| **DEBUG** | 개발 중에만. 운영에서는 끈다 | 파라미터 값, 분기 결과 |

**핵심 원칙: ERROR는 "사람이 조치해야 하는 것"만.**

### 404를 ERROR로 남기면 안 되는 이유

```
봇이 /admin, /wp-login.php, /.env 를 하루 5만 번 긁는다 → 전부 404
      ▼  이걸 ERROR로 남기면
ERROR 로그 5만 건  →  진짜 ERROR 1건이 묻힌다
      ▼
알림이 계속 울리니 사람들이 알림을 끈다  →  진짜 장애를 아무도 모른다 💀
```

이것을 **알림 피로(alert fatigue)** 라 한다. **ERROR를 남발하면 ERROR가 무의미해진다.**

### 이번 과제의 기준

| 상황 | 누구 잘못 | 레벨 |
|---|---|---|
| 404, 400 | **클라이언트** | `WARN` (또는 남기지 않음) |
| 500 (예상 못 한 예외) | **서버** | **`ERROR` + 스택트레이스** |

### ⚠️ 스택트레이스를 남기는 법 — 흔한 실수

```java
log.error("처리 중 오류 발생", e);                 // ✅ 스택트레이스가 찍힌다
log.error("처리 중 오류 발생: " + e.getMessage()); // ❌ 어디서 터졌는지 사라진다
```
**예외 객체를 마지막 인자로 넘겨야** 스택트레이스가 남는다. 500 디버깅에 제일 필요한 정보다.

### 추가 규칙

**① 문자열 연결 대신 `{}` 플레이스홀더**
```java
log.debug("게시글 조회 postId={}", postId);   // ✅
log.debug("게시글 조회 postId=" + postId);     // ❌ DEBUG가 꺼져 있어도 문자열 연결은 실행된다
```

**② 민감 정보(비밀번호·토큰 등)를 로그에 찍지 않는다.** 로그는 평문으로 오래 보관되고 여러 사람이 본다.

---

## 쿼리 수 측정 기록 ⭐

### 측정 환경
- 게시글 10개, 각 게시글당 댓글 3개 (총 댓글 30개)

### 결과

| API | 해결 전 쿼리 수 | 해결 후 쿼리 수 | 비고 |
|---|---|---|---|
| `GET /posts?page=0&size=10` (offset) | | | |
| `GET /posts?cursor=&size=10` (cursor) | - | | 2단계 |
| `GET /posts/{id}` | | | |

### 왜 그렇게 나왔는가
-

### 어떻게 해결했는가
-

### 고려했다가 버린 방법과 그 이유
-

---

## 구현 메모

(막힌 지점, 원인, 받은 힌트 단계)

| # | 막힌 지점 | 원인 / 교훈 |
|---|---|---|
| | | |

---

## 리뷰 질문과 내 답변

(모의 면접 Q&A는 같은 폴더의 `interview-log.md`에 정리)

---

## 회고

- 잘한 점:
- 부족한 점:
- 다음에 다르게 할 것:

---

## 예상 면접 질문

1.
2.
3.
