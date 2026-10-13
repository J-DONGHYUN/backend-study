---
제목: 댓글을 저장했는데 응답의 id가 null이었습니다
작성: 2026-10-12
발행:
출처: assignments/2026-10-08-board
---

게시판 API를 만들다 만난 일입니다.

## 문제

댓글 등록 API를 호출했더니 이런 응답이 왔습니다.

```json
POST /posts/1/comments

{"id":null,"content":"첫 댓글","author":"tester","createdAt":null}
```

`id`도 `createdAt`도 `null`입니다.

저장이 안 된 줄 알고 바로 조회해봤습니다.

```json
GET /posts/1

{"comments":[{"id":1,"content":"첫 댓글","createdAt":"2026-10-08T20:49:38"}]}
```

**멀쩡히 있습니다.** DB는 정상이고 **응답만 틀렸습니다.**

## 코드

```java
@Transactional
public CommentResponse addComment(Long postId, CommentCreateRequest request) {
    Post post = findPost(postId);
    Comment comment = post.addComment(request.content(), request.author());
    return CommentResponse.from(comment);        // ← 여기
}
```

`post.addComment()`는 이게 전부입니다.

```java
public Comment addComment(String content, String author) {
    Comment comment = new Comment(this, content, author);
    this.comments.add(comment);                  // 컬렉션에 추가만 한다
    return comment;
}
```

## 원인 — JPA는 변경을 모아뒀다가 한 번에 보냅니다

```
┌─ @Transactional 시작 ────────────────────────────┐
│                                                  │
│  post.addComment(...)                            │
│     └─ 컬렉션에 추가만. INSERT 는 아직 안 나간다      │
│        comment.id = null                         │
│                                                  │
│  CommentResponse.from(comment)    ★ DTO 생성     │
│     └─ new CommentResponse(null, "첫 댓글", null) │
│                                                  │
└─ 커밋 ───────────────────────────────────────────┘
        │
        ├─ flush
        │    ├─ cascade 로 persist 전이
        │    ├─ @PrePersist 실행  → createdAt 설정
        │    └─ INSERT INTO comment → id = 1 할당
        │
        │   ✅ Comment 엔티티는 이제 완전하다
        │   ❌ 하지만 ★ 에서 만든 DTO 는 그대로다
        ▼
   {"id":null, "createdAt":null}
```

JPA는 변경을 바로 DB로 보내지 않습니다. **모아뒀다가 flush 시점(보통 커밋)에 한 번에** 보냅니다. **쓰기 지연(write-behind)** 이라고 합니다.

쿼리를 모아 보내면 DB 왕복이 줄어서 빠릅니다. 대신 **커밋 전에 엔티티를 읽으면 아직 채워지지 않은 값이 있습니다.**

## 핵심 — DTO는 스냅샷입니다

```
  comment (엔티티)                  CommentResponse (DTO)
  ┌────────────────┐               ┌────────────────┐
  │ id   = null    │ ─── 복사 ───▶  │ id   = null    │
  │ ...            │               │ ...            │
  └────────────────┘               └────────────────┘
          │                                 │
     flush 후                          record 는 불변
          ▼                                 ▼
  ┌────────────────┐               ┌────────────────┐
  │ id   = 1   ✏️  │               │ id   = null    │  ← 안 바뀐다
  └────────────────┘               └────────────────┘
```

엔티티는 나중에 채워지지만, **이미 값을 복사해 간 DTO는 바뀌지 않습니다.**

## 해결

응답을 만들기 전에 flush를 명시하면 됩니다.

```java
Comment comment = post.addComment(request.content(), request.author());
postRepository.flush();                 // INSERT 를 지금 실행
return CommentResponse.from(comment);
```

순서만 바꾼 것입니다. **INSERT를 먼저 보내고 DTO를 만듭니다.**

## 그런데 게시글 등록은 왜 멀쩡했을까

같은 구조인데 `POST /posts`는 `id`가 잘 나왔습니다. 차이는 **누가 persist를 호출하느냐**였습니다.

```
 postRepository.save(post)          내가 "직접" persist 를 호출
      └─ @GeneratedValue(IDENTITY) 라 INSERT 를 미룰 수 없다
         (id 를 알려면 INSERT 를 해봐야 하니까)
      └─ 그 자리에서 INSERT → id 즉시 할당  ✅

 post.addComment(...)               아무도 persist 를 호출하지 않는다
      └─ cascade 는 "flush 시점에" 전이된다
      └─ 그때까지 Comment 는 비영속 상태  ❌
```

| | persist 시점 | flush 필요? |
|---|---|---|
| `create()` | **즉시** (`save()` 직접 호출) | ❌ |
| `addComment()` | **flush 때** (cascade 전이) | ✅ |
| `update()` | **flush 때** (변경 감지) | ✅ |

`update()`도 같은 문제가 있었습니다. 수정 응답의 `updatedAt`이 **갱신 전 값**으로 나갔습니다. `@PreUpdate`가 flush 때 실행되기 때문입니다.

## 배운 것

> **DTO를 만드는 시점에, JPA가 아직 하지 않은 일은 DTO에 담기지 않습니다.**

| 값 | 누가 채우나 | 커밋 전에 있나 |
|---|---|---|
| `title`, `content` | **내가 직접 대입** | ✅ |
| `id` (IDENTITY) | **DB** (INSERT 후) | cascade 면 ❌ |
| `createdAt` | `@PrePersist` | cascade 면 ❌ |
| `updatedAt` | `@PreUpdate` (flush 때) | ❌ |

내가 직접 대입한 값은 바로 반영되고, **JPA 콜백이 채우는 값만 늦습니다.**

그리고 더 무서웠던 건 이겁니다. **DB는 항상 맞았습니다. 틀린 건 클라이언트가 받는 응답뿐이었습니다.**
`id`가 `null`인 건 바로 눈에 띄었지만, `updatedAt`이 "한 박자 전 날짜"인 건 그럴듯해서 한참 몰랐습니다.

---
전체 코드: https://github.com/J-DONGHYUN/backend-study
