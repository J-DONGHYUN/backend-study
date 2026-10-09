# 로그 읽기 & 로깅 설계

> 참고용 정리. 읽고 나서 **내 말로 요약한 노트는 따로** 작성할 것.

---

## 1. 로그 한 줄의 구조

Spring Boot 기본 포맷이다.

```
2026-10-08T20:49:38.834+09:00  WARN 12345 --- [board] [io-8080-exec-1] c.b.g.e.GlobalExceptionHandler : 게시글을 찾을 수 없음: id=999
└──────── ① ────────────────┘ └ ② ┘ └ ③ ┘     └─ ④ ─┘ └────── ⑤ ─────┘ └────────── ⑥ ──────────────┘ └──────── ⑦ ────────┘
```

| | 뭔가 | 언제 쓰나 |
|---|---|---|
| ① 시각 | 타임스탬프 | **장애 시각과 로그를 맞출 때.** 제일 먼저 보는 것 |
| ② 레벨 | ERROR/WARN/INFO/DEBUG | 필터링의 기준 |
| ③ PID | 프로세스 id | 서버에 여러 앱이 뜰 때 구분 |
| ④ 앱 이름 | `spring.application.name` | 여러 서비스의 로그가 섞일 때 |
| ⑤ **스레드 이름** | `io-8080-exec-1` | **요청 하나 = 스레드 하나.** 같은 요청의 로그를 묶어볼 때 |
| ⑥ 로거 | 클래스명(축약) | **어디서 찍었나.** `c.b.p.s.PostService` = `com.board.post.service.PostService` |
| ⑦ 메시지 | 내가 쓴 내용 | |

**⑤ 스레드 이름이 특히 유용하다.** 동시에 여러 요청이 들어오면 로그가 뒤섞이는데,
`io-8080-exec-1` 로 grep 하면 **그 요청 하나의 흐름만** 뽑아볼 수 있다.

---

## 2. 스택트레이스 읽는 법 ⭐ 가장 중요한 기술

### 예시

```
org.springframework.dao.InvalidDataAccessApiUsageException: Post not found
    at org.springframework.orm.jpa.EntityManagerFactoryUtils.convertJpaAccessException(...)
    at org.springframework.orm.jpa.vendor.HibernateJpaDialect.translateException(...)
    at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(...)
    at com.board.post.service.PostService.update(PostService.java:52)          ← ★ 내 코드
    at com.board.post.controller.PostController.update(PostController.java:61) ← ★ 내 코드
    at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(...)
    at org.springframework.web.servlet.DispatcherServlet.doDispatch(...)
    ... 60 more
Caused by: java.lang.IllegalArgumentException: Post with id 999 does not exist   ← ◆ 진짜 원인
    at com.board.post.service.PostService.lambda$findByPostId$0(PostService.java:78)
    at java.base/java.util.Optional.orElseThrow(Optional.java:403)
    ... 65 more
```

### 읽는 순서 3단계

```
 1단계   맨 아래 "Caused by" 로 먼저 간다
           └─ Caused by 가 여러 개면 가장 마지막 것이 진짜 원인
              위쪽 예외들은 그걸 감싸서 다시 던진 것일 뿐이다

 2단계   그 Caused by 블록에서 "내 패키지"로 시작하는 첫 줄을 찾는다
           └─ com.board... ← 범인은 거의 항상 여기
              org.springframework / java.base 줄은 건너뛴다

 3단계   파일명:줄번호 로 바로 점프한다
           └─ PostService.java:78
```

**초보가 하는 실수: 맨 윗줄만 읽는다.**
맨 위는 "가장 바깥에서 감싼 예외"라 **진짜 원인이 아닌 경우가 많다.**

### `... 60 more` 는 뭔가

앞의 스택과 **중복되는 부분을 생략**한 것이다. 무시해도 된다.

### IntelliJ 팁
- 콘솔의 `PostService.java:78` 은 **클릭하면 그 줄로 이동**한다.
- `Cmd+F` 로 콘솔에서 `com.board` 를 검색하면 내 코드 줄만 빠르게 찾는다.

---

## 3. 로그 레벨 — 남기는 쪽 관점

**판단 기준 하나: "이 로그를 누가, 언제 보는가?"**

| 레벨 | 누가 언제 보나 | 운영에서 켜나 |
|---|---|---|
| **ERROR** | **지금 조치가 필요.** 알림이 울려야 한다 | ✅ |
| **WARN** | 이상하지만 서비스는 돈다. 나중에 확인 | ✅ |
| **INFO** | "무슨 일이 있었나" 흐름 추적 | ✅ |
| **DEBUG** | 개발 중 상세. 파라미터, 분기 | ❌ 보통 끈다 |
| TRACE | 아주 상세. SQL 파라미터 등 | ❌ |

**핵심 원칙: ERROR는 "사람이 조치해야 하는 것"만.**

```
봇이 /admin, /.env 를 하루 5만 번 긁는다 → 전부 404
      ▼  이걸 ERROR 로 남기면
ERROR 5만 건  →  진짜 ERROR 1건이 묻힌다
      ▼
알림이 계속 울려서 사람들이 알림을 끈다  →  진짜 장애를 놓친다 💀
```
→ **알림 피로(alert fatigue).** ERROR를 남발하면 ERROR가 무의미해진다.

| 상황 | 누구 잘못 | 레벨 |
|---|---|---|
| 4xx (없는 리소스, 검증 실패) | **클라이언트** | `WARN` 또는 남기지 않음 |
| 5xx (예상 못 한 예외) | **서버** | **`ERROR` + 스택트레이스** |

---

## 4. 반드시 지킬 것 3가지

### ① 예외 객체를 마지막 인자로 넘긴다

```java
log.error("처리 중 오류 발생", e);                  // ✅ 스택트레이스가 찍힌다
log.error("처리 중 오류 발생: " + e.getMessage());  // ❌ 어디서 터졌는지 사라진다
```

**500 디버깅에 제일 필요한 정보가 스택트레이스다.** 문자열로 이어붙이면 그게 통째로 날아간다.

### ② 문자열 연결 대신 `{}` 플레이스홀더

```java
log.debug("게시글 조회 postId={}", postId);   // ✅
log.debug("게시글 조회 postId=" + postId);     // ❌
```

두 번째는 **DEBUG가 꺼져 있어도 문자열 연결이 먼저 실행된다.** 로그를 안 찍는데 비용은 낸다.

### ③ 민감 정보를 남기지 않는다

비밀번호, 토큰, 주민번호, 카드번호.
**로그는 평문으로 오래 보관되고 여러 사람이 본다.** 로그 유출이 곧 개인정보 유출이 된다.

---

## 5. Hibernate SQL 로그 읽기

### 설정

```yaml
spring:
  jpa:
    show-sql: true                      # SQL 출력
    properties:
      hibernate:
        format_sql: true                # 줄바꿈해서 보기 좋게
        generate_statistics: true       # 쿼리 수 통계 수집

logging:
  level:
    org.hibernate.stat: debug           # 통계를 "출력"하려면 필요 (수집과 출력은 별개)
    org.hibernate.orm.jdbc.bind: trace  # ? 에 들어간 실제 값 보기
```

> `generate_statistics` 만 켜면 수집만 되고 **안 보인다.** `org.hibernate.stat: debug` 가 한 쌍이다.
> 둘 다 **개발 중에만** 켠다. 운영에서는 오버헤드가 있다.

### 파라미터 값 보기

```
Hibernate:
    select p1_0.id, p1_0.title from post p1_0 where p1_0.id=?
                                                          ↑ 이게 뭔지 모른다

org.hibernate.orm.jdbc.bind: trace 를 켜면
    binding parameter (1:BIGINT) <- [999]                 ← 999 였구나
```

**"쿼리는 맞는 것 같은데 결과가 이상할 때"** 가장 먼저 확인할 것.

### Session Metrics — 쿼리 수 세기

```
Session Metrics {
    ...
    11 JDBC statements            ← 이 요청이 날린 쿼리 수
    ...
}
```

**N+1 을 찾는 가장 확실한 방법.** 게시글 10건을 조회했는데 11개가 나왔다면 `1 + 10` 이다.

---

## 6. 로그 검색 (터미널)

```bash
# 에러만
grep "ERROR" app.log

# 특정 요청(스레드) 흐름만
grep "io-8080-exec-1" app.log

# 에러와 그 아래 20줄(스택트레이스)
grep -A 20 "ERROR" app.log

# 특정 시각 구간
grep "2026-10-08T20:4" app.log

# 내 코드가 등장하는 줄만
grep "com.board" app.log
```

**`-A`(after), `-B`(before), `-C`(양쪽) 가 로그 볼 때 제일 많이 쓴다.**

---

## 7. 한 걸음 더: 요청 추적 (MDC)

동시에 100개 요청이 들어오면 로그가 뒤섞인다. 스레드 이름으로 구분할 수 있지만,
**서버가 여러 대이거나 비동기 처리가 끼면 그마저 깨진다.**

그래서 요청마다 **고유 id(traceId)** 를 붙인다.

```
[traceId=a3f2b1] 게시글 조회 시작 postId=1
[traceId=a3f2b1] 댓글 3건 로딩
[traceId=a3f2b1] 응답 완료 200
```

SLF4J의 **MDC(Mapped Diagnostic Context)** 로 구현한다.
실무에서는 Spring Cloud Sleuth / Micrometer Tracing 이 자동으로 해준다.
**지금은 "이런 게 있다"만 알아두면 된다.**

---

## 안티패턴

| 하지 말 것 | 왜 |
|---|---|
| `e.printStackTrace()` | 로그 레벨·포맷·파일 설정을 전부 무시하고 표준출력으로 나간다. **운영에서 수집이 안 된다** |
| `System.out.println()` | 같은 이유 |
| 모든 걸 INFO로 | 정작 중요한 게 안 보인다 |
| 예외를 잡고 로그만 찍고 삼키기 | 호출한 쪽은 성공한 줄 안다. 가장 위험한 패턴 |
| 루프 안에서 로그 | 1만 건 처리하면 로그 1만 줄 |

---

## 면접 포인트

1. **로그 레벨을 어떤 기준으로 나누나?** → "조치가 필요한가"로 나눈다. ERROR는 사람이 즉시 봐야 하는 것만. 4xx는 클라이언트 잘못이라 WARN 이하.
2. **`log.error("msg", e)` 와 `log.error("msg: " + e.getMessage())` 의 차이는?** → 전자는 스택트레이스가 남고 후자는 안 남는다.
3. **`{}` 를 쓰는 이유는?** → 해당 레벨이 꺼져 있으면 문자열 조합 자체를 하지 않는다.
4. **스택트레이스에서 원인을 어떻게 찾나?** → 맨 아래 `Caused by` 로 가서, 내 패키지로 시작하는 첫 줄을 본다.
