# Interview Log — 게시판 API (과제 #2)

> 구현하면서 **실제로 헷갈렸던 것들**을 복습용으로 정리.
> 과제 개요·설계는 같은 폴더의 `README.md` 참고.

---

# 1. flush — 왜 update 에는 넣고 create 에는 안 넣었나 ⭐

## 겪은 증상

```
댓글 등록 응답   {"id":null, "content":"첫 댓글", "createdAt":null}    ← id 가 없다
게시글 수정 응답  createdAt=.381184  updatedAt=.381184                 ← 수정했는데 안 바뀜
게시글 등록 응답  {"id":1, "createdAt":"...", "updatedAt":"..."}        ← 이건 멀쩡
```

**세 가지가 왜 다른가?**

## 핵심 한 줄

> **DTO 는 그 순간의 값을 복사한 스냅샷이다.**
> `record` 는 불변이라, 나중에 엔티티가 갱신돼도 **이미 만들어진 DTO 는 바뀌지 않는다.**

## ① create — flush 불필요

```java
Post savedPost = postRepository.save(post);     // persist 를 "내가 직접" 호출
return PostResponse.from(savedPost);
```

```
 postRepository.save(post)
    └─ em.persist(post)              ← persist 가 지금 호출된다
         ├─ @PrePersist 실행          → createdAt 설정 ✓
         └─ INSERT INTO post ...      → 즉시 실행 (IDENTITY 전략)
              └─ DB 가 돌려준 id 할당  → id 설정 ✓

 PostResponse.from(savedPost)        ← 이미 다 채워져 있다
```

**`@GeneratedValue(strategy = IDENTITY)` 라서 INSERT 를 미룰 수 없다.**
영속성 컨텍스트는 엔티티를 **id 를 키로** 1차 캐시에 보관하는데,
IDENTITY 는 **DB 가 id 를 만들므로 INSERT 를 해봐야 id 를 안다.**

## ② addComment — flush 필요

```
┌─ @Transactional 시작 ──────────────────────────────────────┐
│                                                            │
│  post.addComment("첫 댓글", "tester")                       │
│     ├─ new Comment(...)        → 비영속 상태                 │
│     └─ comments.add(comment)   → 메모리 컬렉션에만 추가        │
│        ❗ persist() 호출 안 됨 → INSERT 없음 → id = null      │
│                                                            │
│  CommentResponse.from(comment)   ★ 여기서 DTO 생성           │
│     └─ new CommentResponse(null, "첫 댓글", null, null)      │
│        ❗ record = 불변. 지금 복사한 값이 끝이다               │
│                                                            │
└─ 트랜잭션 커밋 ────────────────────────────────────────────┘
      │
      ├─ flush
      │    ├─ 컬렉션 변경 감지 → cascade PERSIST 전이
      │    ├─ @PrePersist 실행     → createdAt 설정
      │    └─ INSERT INTO comment → id = 1 할당
      │
      │    ✅ Comment 엔티티는 완전해짐 (DB 저장도 정상)
      │    ❌ 하지만 ★ 에서 만든 DTO 는 여전히 null
      ▼
 {"id":null, "createdAt":null}
```

**`cascade` 는 "flush 시점에 부모 컬렉션을 검사해서 전이"한다.**
그 전까지 Comment 는 **비영속 상태로 컬렉션에 들어있을 뿐**이다.

## ③ update — flush 필요

```
┌─ @Transactional 시작 ──────────────────────────────────────┐
│  findByPostId(1) → post 는 영속 상태                         │
│                                                            │
│       1차 캐시              스냅샷(최초 상태 복사본)           │
│       title="첫 글"         title="첫 글"                    │
│       updatedAt=.8347      updatedAt=.8347                 │
│                                                            │
│  post.update("수정된 제목", "수정된 본문")                     │
│                                                            │
│       1차 캐시                스냅샷                          │
│       title="수정된 제목"     title="첫 글"      ← 달라짐       │
│       updatedAt=.8347        updatedAt=.8347   ← 아직 그대로   │
│        ❗ @PreUpdate 미실행 → updatedAt 은 옛날 값             │
│                                                            │
│  PostResponse.from(post)        ★ DTO 생성                  │
│     title ✅ 맞음  /  updatedAt ❌ 틀림                       │
└─ 트랜잭션 커밋 ────────────────────────────────────────────┘
      │
      ├─ flush
      │    ├─ 스냅샷과 비교 → 변경 감지(dirty checking)
      │    ├─ @PreUpdate 실행 → updatedAt 갱신
      │    └─ UPDATE post SET ...
```

**title 은 맞는데 updatedAt 만 틀렸던 이유:**
내가 직접 대입한 값은 즉시 반영되고, **JPA 콜백이 채우는 값만 늦는다.**

## 표로 정리

| 값 | 누가 채우나 | DTO 생성 시점에 있나 |
|---|---|---|
| `title`, `content` | **내가 직접 대입** | ✅ |
| `id` (IDENTITY) | **DB** (INSERT 후) | ❌ cascade 면 flush 전엔 없음 |
| `createdAt` | `@PrePersist` | ❌ cascade 면 flush 전엔 없음 |
| `updatedAt` | `@PreUpdate` (**flush 때**) | ❌ flush 전엔 옛날 값 |

| 메서드 | persist/반영 시점 | flush 필요? |
|---|---|---|
| `create()` | **즉시** (`save()` 직접 호출 + IDENTITY) | ❌ |
| `addComment()` | **flush 때** (cascade 전이) | ✅ |
| `update()` | **flush 때** (변경 감지) | ✅ |
| `delete()` | flush 때 | ❌ (응답에 돌려줄 게 없음) |

## 실무에서는? — 항상 flush 를 넣는 게 답은 아니다

| 방법 | 설명 |
|---|---|
| ① **응답에 그 값이 필요한지 재검토** | 수정 API 가 204 만 주면 문제 자체가 없어진다. **가장 흔한 해결** |
| ② `flush()` 명시 | 우리가 쓴 방법. 배치의 이점을 포기 |
| ③ 자식도 Repository 로 직접 `save` | id 를 즉시 얻음. 대신 Aggregate 경계가 느슨해짐 |
| ④ 시각을 콜백 대신 도메인 메서드에서 직접 설정 | 모든 변경 메서드에서 빠짐없이 갱신해야 함 |

> **면접:** "`save()` 를 호출했는데 바로 DB 에 반영되지 않는 이유는?"
> → **쓰기 지연.** 영속성 컨텍스트가 변경을 모아뒀다가 **flush 시점(보통 커밋)** 에 한 번에 보낸다.
> 쿼리를 모아 보내 배치 처리를 할 수 있어서다. 즉시 필요하면 `flush()` 를 명시한다.

---

# 2. N+1 — N 과 1 이 각각 뭔가 ⭐

## 처음 오해한 것

> ❌ "1번만 하려는데 N번이 생긴다" → **전체 쿼리가 1개여야 한다고 착각**

## 바른 이해

```
전체 쿼리 = [원래 필요한 것]  +  [N+1 때문에 생긴 것]

  목록 조회 (페이징)
      ├─ select from post        ← 원래 필요  ★ 이게 "1"
      ├─ select count from post  ← 페이징의 정상 비용 (N+1 논의 밖)
      └─ select from comment ×N  ← 💥 이게 "N"
```

**"1" 은 "전체가 1개여야 한다"가 아니라 "부모를 가져오는 그 쿼리 하나"를 가리킨다.**
거기에 자식 조회가 N번 **덧붙는** 현상이라 N+1 이다.

## N 은 "부모 건수"다 — 자식 건수가 아니다

```
게시글 3개 · 각 댓글 1개   (댓글 3개)    →  쿼리 수 동일
게시글 3개 · 각 댓글 3개   (댓글 9개)    →  쿼리 수 동일
게시글 3개 · 각 댓글 100개 (댓글 300개)  →  쿼리 수 동일

where post_id = 1    ← 이 쿼리 하나가 1번 게시글의 댓글을 "전부" 가져온다
```

**게시글 하나당 쿼리 하나.** 그래서 게시글이 늘어야 쿼리가 는다.

## 범인은 이 한 줄

```java
// PostListResponse.from()
post.getComments().size()     // ★
```

```
posts.map(PostListResponse::from)
      │  게시글 1건마다 from() 호출
      ▼
 post.getComments()           // 여기까진 쿼리 없음 (프록시만 꺼냄)
 post.getComments().size()    // 여기서 나감 ← 실제로 컬렉션을 들여다봄
      ▼
 select ... from comment where post_id = ?
```

## 목표를 다시 정의하면

> **N+1 해결의 목표는 "쿼리 1개"가 아니라, 데이터가 늘어도 쿼리 수가 안 늘어나게 만드는 것.**

| 게시글 수 | 배치 없음 | 배치 100 |
|---|---|---|
| 3개 | 2 + **3** = 5 | 2 + **1** = 3 |
| 10개 | 2 + **10** = 12 | 2 + **1** = 3 |
| 100개 | 2 + **100** = 102 | 2 + **1** = 3 |
| 1000개 | 2 + **1000** = 1002 | 2 + **10** = 12 |

---

# 3. 프록시를 "깨운다"는 게 뭔가

## 깨우는 기준 = "프록시가 가진 실제 데이터가 필요한 순간"

### 컬렉션 프록시
```java
post.getComments()              // ❌ 안 깨움 — 프록시 참조만 돌려줌
post.getComments().size()       // ✅ 깨움
post.getComments().stream()     // ✅ 깨움
post.getComments().get(0)       // ✅ 깨움
```

### 엔티티 프록시
```java
comment.getPost()            // ❌ 안 깨움
comment.getPost().getId()    // ❌ 안 깨움!  ← 외래키가 comment 테이블에 이미 있다
comment.getPost().getTitle() // ✅ 깨움
```

> **실무 팁:** 연관 엔티티의 **id 만 필요하면 쿼리가 아예 안 나간다.** 공짜다.
> 이걸 모르면 불필요하게 fetch join 을 건다.

---

# 4. 배치 사이즈 — "로딩 대기 중인 프록시" 가 뭔 소린가

## 목록 조회 직후 영속성 컨텍스트 상태

```
 select ... from post ... limit 10     ← 이 쿼리 하나가 끝난 시점

 ┌──────────── 영속성 컨텍스트 (1차 캐시) ────────────┐
 │  Post(id=1)  comments → [프록시 · 미초기화 · key=1 ] ┐
 │  Post(id=2)  comments → [프록시 · 미초기화 · key=2 ] │ 전부
 │  Post(id=3)  comments → [프록시 · 미초기화 · key=3 ] ├ "Post.comments" 종류
 │     ...                                             │ 전부 아직 비어있음
 │  Post(id=10) comments → [프록시 · 미초기화 · key=10] ┘ = "로딩 대기 중"
 └─────────────────────────────────────────────────────┘
```

게시글 10개는 올라왔고, 각 `comments` 는 **빈 껍데기(프록시)**.
각 프록시는 **"나는 post_id = N 의 댓글이다"라는 열쇠(key)만** 들고 있다.

## 첫 프록시를 깨울 때

```
 post1.getComments().size()
        │
   ┌────┴──────────────────────────────────────────┐
 [배치 없음]                                  [배치 100]
   │ 시킨 것만                        영속성 컨텍스트를 훑는다
   ▼                                 "Post.comments 종류인데 미초기화인 게 또 있나?"
 where post_id = 1                          → key 2,3,...,10 발견
   │                                                │
 post2 → 또 쿼리 💥                                  ▼
 post3 → 또 쿼리 💥                      where post_id IN (1,2,...,10)
                                                    │ 10개가 전부 채워짐
                                                    ▼
                                         post2.getComments() → 쿼리 없음 ✅
```

## 핵심 한 줄

> **배치 사이즈는 "지금 당장 필요한 것"보다 많이 가져온다. 어차피 곧 다 필요할 테니까.**

## 설정

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100     # 전역. 실무에서 가장 많이 쓰는 방식
```
```java
@BatchSize(size = 100)      // 개별 연관관계만 다르게 할 때
@OneToMany(mappedBy = "post", ...)
```

**값은 보통 100 또는 1000.** 너무 작으면 효과가 적고, 너무 크면 `IN` 절이 길어져 DB 가 부담을 느낀다(Oracle 은 1000개 제한).

## 왜 실무에서 많이 쓰나

| | 쿼리 수 | 페이징 | 코드 수정 |
|---|---|---|---|
| **batch size** | 1 + 1 | ✅ 안전 | **설정 한 줄** |
| fetch join | 1 | ⚠️ **컬렉션이면 메모리 페이징 위험** | 쿼리 작성 필요 |

**조건:** 같은 영속성 컨텍스트(= 같은 트랜잭션)에 프록시들이 모여 있어야 효과가 난다.
→ 목록 조회에서 극적. 단건 조회는 프록시가 1개뿐이라 효과 없음.

> **`FetchType.EAGER` 는 답이 아니다.** EAGER 로 바꿔도 Hibernate 는 여전히 게시글마다 따로 가져온다.
> "미리 가져온다"일 뿐 "한 번에 가져온다"가 아니다. 쿼리 수는 그대로이고, 안 쓸 때도 가져오니 더 나쁘다.

---

# 5. 왜 댓글 등록/삭제를 Post 에서 하나 (Aggregate)

## Aggregate / Aggregate Root

**함께 변경되어야 하는 객체들의 묶음**이 Aggregate, 그 **입구가 되는 객체**가 Root.

```
┌─ Aggregate (Post) ──────────────┐
│    Post  ← Aggregate Root       │
│      ├─ Comment                 │      ▲
│      ├─ Comment                 │      │ 외부는 반드시
│      └─ Comment                 │      │ 루트를 통해서만 접근
└─────────────────────────────────┘   Service
```

## 왜 Post 가 루트인가 — 내 설계가 이미 그렇게 정했다

1. **`cascade = ALL, orphanRemoval = true` 를 골랐다** → **컬렉션을 통한 삭제를 전제**한다
   ```java
   post.getComments().remove(comment);   // ✅ orphanRemoval 작동
   commentRepository.delete(comment);     // ❌ orphanRemoval 과 무관 → 선언만 하고 안 쓰는 셈
   ```
2. **"댓글은 게시글 없이 존재 의미가 없다"** 고 판단했다 → **같은 Aggregate** 라는 뜻
3. **양방향 동기화를 빠뜨릴 수 없게 된다**

## 따라오는 결과: `CommentRepository` 가 없다

> **Repository 는 Aggregate Root 마다 하나.** 내부 객체는 자기 Repository 를 갖지 않는다.

| 할 일 | 어떻게 | Repository 필요? |
|---|---|---|
| 댓글 등록 | `post.addComment(...)` → **cascade** 가 저장 | ❌ |
| 댓글 삭제 | `post.removeComment(id)` → **orphanRemoval** 이 삭제 | ❌ |
| 댓글 조회 | `post.getComments()` | ❌ |

```java
// Service — commentRepository 가 한 번도 안 나온다
public void deleteComment(Long postId, Long commentId) {
    Post post = findByPostId(postId);
    post.removeComment(commentId);
}
```

## 정직한 반대 논거

```java
public void removeComment(Long commentId) {
    this.comments.remove(target);   // ← 이 순간 comments 전체가 DB 에서 로딩된다
}
```
**댓글이 10만 개면 10만 건을 메모리에 올리고 1건을 지운다.**
그래서 댓글이 많아지는 서비스에서는 Comment 를 **별도 Aggregate** 로 분리한다.

| | Post 가 루트 (이 과제) | Comment 독립 |
|---|---|---|
| cascade/orphanRemoval | 사용 | 사용 안 함 |
| 소속 검증 | 컬렉션이 알아서 | Service 에서 직접 |
| 댓글 10만 개 | **전체 로딩 💥** | 문제없음 |
| 일관성 보장 | 강함 | 직접 챙겨야 함 |

> **면접:** "댓글 삭제를 왜 `Post` 를 통해서 하셨나요?"
> → "게시글과 댓글을 **하나의 Aggregate 로 보고 Post 를 루트로** 잡았습니다.
> `orphanRemoval` 로 일관성을 보장하고 양방향 동기화를 한 곳에서 처리할 수 있습니다.
> 다만 **댓글이 아주 많아지면 컬렉션 전체가 로딩되는 문제**가 있어서,
> 그 규모에서는 별도 Aggregate 로 분리하는 게 맞다고 봅니다."

---

# 6. 왜 도메인에서 상태를 바꾸나

## Service 에 두면 생기는 일

```java
// CommentService
post.getComments().add(comment);     // ① 컬렉션 추가

// 나중에 AdminService 추가 → 똑같이 써야 함
// 나중에 ImportService 추가 → commentRepository.save(comment) 만 하고
//                            컬렉션 추가를 빠뜨림 💥
```
→ 같은 트랜잭션에서 게시글을 조회하면 **방금 단 댓글이 안 보인다.**

**"양쪽을 맞춰야 한다"는 건 `Post` 의 내부 사정**인데, 그걸 아는 책임을 **밖의 모든 Service 에 떠넘긴** 결과.

## 논거 3개

1. **규칙이 흩어지지 않는다** — Service 는 늘어나고 Entity 는 하나다
2. **묻지 말고 시켜라(Tell, Don't Ask)** — `post.getComments().size() >= 100` 은 내부 구조를 밖이 아는 것
3. **테스트가 쉬워진다** — 과제 #1 의 `TodoTest` 가 Spring/DB 없이 밀리초에 돌았던 이유

## 경계선

| Entity | Service |
|---|---|
| **자기 상태에 대한 규칙** | **여러 엔티티에 걸친 조율** |
| `complete()`, `update()`, `addComment()` | 조회 → 변경 → 저장의 흐름 |
| 자기 데이터만 보면 판단 가능 | **트랜잭션 경계**, DB 조회, 외부 API |

**판단 기준:** *"이 판단에 자기 데이터 말고 다른 게 필요한가?"*

> 용어: **Anemic Domain Model**(데이터만 든 빈혈 모델) ↔ **Rich Domain Model**(자기 규칙을 가짐).
> Martin Fowler 는 anemic 을 안티패턴이라 했다. 다만 실무엔 논쟁이 있고, **일관성과 이유가 더 중요**하다.

---

# 7. 연관관계 — 자주 틀린 것들

## `mappedBy` 에는 **상대편의 필드 이름**을 쓴다

```java
public class Comment {
    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;                    // ← 이 필드 이름이 "post"
}

public class Post {
    @OneToMany(mappedBy = "post")         // ← 그 이름을 그대로. 클래스명 아님!
    private List<Comment> comments;
}
```
뜻: **"나는 주인이 아니다. 외래키 관리는 Comment 의 `post` 필드가 한다."**

## 주인 = 외래키를 가진 쪽

**주인이 아닌 `Post.comments` 는 읽기 전용.**
`post.getComments().add(c)` 만 하고 `c.setPost(post)` 를 빠뜨리면 **DB 에 반영되지 않는다.**

## `FetchType` 기본값이 서로 다르다 ⚠️

| | 기본값 |
|---|---|
| `@ManyToOne` | **`EAGER`** ⚠️ |
| `@OneToOne` | **`EAGER`** ⚠️ |
| `@OneToMany` | `LAZY` |
| `@ManyToMany` | `LAZY` |

**실무 원칙: 전부 `LAZY` 로 명시한다.**

## cascade 판단 기준

> **"자식이 부모 없이 혼자 존재할 의미가 있는가?"**

| | cascade |
|---|---|
| 게시글 — 댓글 | ✅ `ALL` + `orphanRemoval` |
| 회원 — 주문 (탈퇴해도 기록은 남아야) | ❌ |
| 팀 — 멤버 | ❌ |

**cascade 는 "단일 소유자 + 생명주기 종속"일 때만.**

## JPA cascade vs DB `ON DELETE CASCADE`

| | JPA | DB |
|---|---|---|
| 주체 | Hibernate | DB |
| 쿼리 수 | 자식 수만큼 DELETE | 1번 |
| 영속성 컨텍스트 동기화 | ✅ | ❌ (캐시와 불일치) |
| `@PreRemove` 콜백 | ✅ | ❌ |

**둘을 동시에 걸면 꼬인다.**

---

# 8. 컬렉션 필드는 선언과 동시에 초기화

```java
private List<Comment> comments = new ArrayList<>();
```

```java
Post post = Post.create("제목", "내용", "작성자");
post.getComments().add(comment);      // 💥 NPE (null.add 를 호출한 셈)
```

**새로 만든 객체의 컬렉션은 아무도 안 채워준다.**
반면 **DB 에서 조회한 객체는 멀쩡하다** — Hibernate 가 자기 컬렉션 구현체를 넣어주니까.
→ **테스트는 통과하는데 등록 기능에서만 터지는** 혼란스러운 상황이 생긴다.

---

# 9. 로그·설정에서 겪은 함정

## `show-sql` 은 로그가 아니다 ⭐

| | `spring.jpa.show-sql: true` | `logging.level.org.hibernate.SQL: debug` |
|---|---|---|
| 출력 경로 | **System.out 직접** | **SLF4J → Logback** |
| 로그 파일에 남나 | ❌ | ✅ |
| 레벨로 제어 | ❌ | ✅ |

**`System.out.println()` 과 같은 문제다.** 콘솔로 볼 땐 괜찮지만 **로그를 파일로 수집하면 SQL 만 사라진다.**

## H2 인메모리는 커넥션이 0이 되면 사라진다

```
Table "POST" not found (this database is empty)
```
```
 앱은 멀쩡히 떠 있는데 데이터만 사라짐
   └─ HikariCP 가 오래된 커넥션을 재활용 (기본 maxLifetime 30분)
        └─ 그 순간 열린 커넥션이 0개가 되면
             └─ H2: "아무도 안 쓰네" → DB 삭제 💥
```
**해결:** `jdbc:h2:mem:boarddb;DB_CLOSE_DELAY=-1`
= "커넥션이 0이어도 **JVM 이 살아있는 동안은 유지**해라"

## `@ExceptionHandler(Exception.class)` 의 함정 ⭐

Spring 은 원래 이런 걸 **올바른 상태 코드로** 처리한다.

| 예외 | Spring 기본 |
|---|---|
| `MethodArgumentTypeMismatchException` (`/posts/abc`) | 400 |
| `HttpMessageNotReadableException` (깨진 JSON) | 400 |
| `HttpRequestMethodNotSupportedException` | 405 |

**그런데 `Exception` 을 잡는 핸들러를 만드는 순간 전부 거기로 빨려 들어가 500 이 된다.**
클라이언트 잘못인데 "서버 잘못"이라 답하는 것이고, **로그도 ERROR 로 오염된다.**

**해결:** 구체적인 예외를 명시적으로 먼저 처리하거나 `ResponseEntityExceptionHandler` 를 상속.

## 500 응답에 `e.getMessage()` 를 노출하지 말 것

```json
{"status":500,"message":"Failed to convert value of type 'java.lang.String' to 'java.lang.Long'"}
```
**내부 구조가 샌다.** DB 예외였다면 테이블명·컬럼명·쿼리가 그대로 나갔을 것.
→ 자세한 건 `log.error("...", e)` 로 **로그에만**, 클라이언트에겐 고정 문구.

## 상태 코드 불일치 (직접 저지른 실수)

```java
ErrorResponse body = ErrorResponse.of(HttpStatus.NOT_FOUND, msg);  // 본문엔 404
return ResponseEntity.badRequest().body(body);                      // 실제론 400 ❌
```
**클라이언트는 보통 HTTP status 로 분기한다.** 400 을 받으면 "내 요청이 잘못됐나" 하고 재시도도 안 한다.

---

# 10. 스택트레이스 읽는 순서

```
 1단계   맨 아래 "Caused by" 로 간다        ← 여러 개면 마지막 것이 진짜 원인
 2단계   그 블록에서 "com.board" 로 시작하는 첫 줄을 찾는다   ← 범인
 3단계   PostService.java:78  클릭해서 점프
```

**초보가 하는 실수: 맨 윗줄만 읽는다.** 맨 위는 "가장 바깥에서 감싼 예외"라 진짜 원인이 아닌 경우가 많다.
`org.springframework`, `java.base` 줄은 대부분 건너뛴다.

실제 예:
```
org.springframework.dao.InvalidDataAccessResourceUsageException: Could not prepare statement
    [Table "POST" not found (this database is empty); ...]
     └──────────── ★ 진짜 원인은 대괄호 안 ────────────┘
```

---

# 복습 체크리스트

소리 내어 답할 수 있는지 확인할 것.

- [ ] `create()` 는 flush 가 필요 없는데 `addComment()` 는 필요한 이유는?
- [ ] 수정 API 응답의 `updatedAt` 이 옛날 값으로 나간 이유는?
- [ ] N+1 에서 N 과 1 은 각각 무엇인가?
- [ ] 댓글이 300개여도 쿼리 수가 안 늘어나는 이유는?
- [ ] 프록시는 언제 깨지는가? `getPost().getId()` 는 왜 안 깨지는가?
- [ ] 배치 사이즈는 어떤 원리로 쿼리를 줄이는가?
- [ ] `EAGER` 로 바꾸면 N+1 이 해결되지 않는 이유는?
- [ ] `CommentRepository` 를 만들지 않은 이유는?
- [ ] `mappedBy` 에는 무엇을 적는가?
- [ ] `@ManyToOne` 의 기본 FetchType 은?
- [ ] cascade 를 써도 되는지 판단하는 기준은?
- [ ] `@ExceptionHandler(Exception.class)` 의 위험은?
