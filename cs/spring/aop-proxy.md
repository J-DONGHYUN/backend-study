# AOP 와 프록시 — @Transactional 은 어떻게 동작하나

## 구술 기록

### 2026-10-22 (4일차) — 🔴 → 🟡  (새 개념)

**Q. `@Transactional` 애노테이션 하나로 트랜잭션이 시작되고 커밋된다. 어떻게 가능한가?**

> 스프링 프레임워크가 해당 메서드가 실행될 때 위 애노테이션을 보고 트랜잭션을 진행한다.
> (힌트 후) 메서드 빈이 따로 있는 건가?

→ "스프링이 본다"까지는 맞지만 **어떻게 끼어드는지**를 모름. 설명 후 이해.

**Q. `@Transactional` 을 Controller 에 붙이면?**

> 트랜잭션을 하는 이유는 보통 DB에 내용을 쓰기, 읽기를 하는 건데 컨트롤러에서는 그러한
> 역할을 하는 부분이 아니기 때문이다. 서비스가 로직을 담당하고 있기 때문에 서비스에서
> 붙이는 거다.

→ **계층 책임으로 정확히 답함.** 커넥션 점유·롤백 범위는 못 댐.

**Q. self-invocation 예시 (쿠폰)**

> 트랜잭션이 안걸림(애노테이션을 안함) / 롤백 불가능

→ 결론 정답.

---

## 1. AOP 가 푸는 문제

```
            create()      update()      delete()
          ┌───────────┬───────────┬───────────┐
  로깅     │   log     │   log     │   log     │ ← 가로로 가로지른다
          ├───────────┼───────────┼───────────┤
  트랜잭션  │  begin    │  begin    │  begin    │
          ├───────────┼───────────┼───────────┤
  진짜 로직 │ 게시글 생성 │ 게시글 수정 │ 게시글 삭제 │ ← 이것만 제각각
          ├───────────┼───────────┼───────────┤
  트랜잭션  │  commit   │  commit   │  commit   │
          └───────────┴───────────┴───────────┘
```

기능은 세로로 나뉘는데 로깅·트랜잭션은 가로로 가로지른다 → **cross-cutting concern(횡단 관심사)**

```
 AOP  =  이 가로줄을 본 코드에서 떼어내 따로 관리하자
```

## 2. 개념이냐 물리적이냐 — 둘 다

```
 AOP      개념 / 패러다임.  언어·프레임워크와 무관
 프록시    스프링이 구현한 방법.  ★ 실제로 메모리에 있는 자바 객체
```

```java
System.out.println(postService.getClass().getName());
// com.board.post.service.PostService$$SpringCGLIB$$0
//                                    ▲ 스프링이 런타임에 만든 클래스
```

**`@Autowired` 로 받은 건 `PostService` 가 아니다.** 그걸 상속한 프록시다.

## 3. 언제 만들어지나

```
 앱 시작
   ├─ @Service 발견
   ├─ "@Transactional 붙은 메서드가 있나?"  →  있다
   ├─ PostService 를 상속한 프록시 클래스를 즉석에서 생성
   └─ 그 프록시 객체를 빈으로 등록   (진짜 객체는 프록시 안에 숨는다)
```

## 4. 런타임 흐름

```
  Controller          프록시               진짜 PostService
      │ update()       │                        │
      │ ─────────────▶ │ ① 트랜잭션 시작           │
      │                │ ② target.update() ────▶ │ 로직
      │                │ ◀───────────────────── │
      │ ◀───────────── │ ③ 커밋 (예외면 롤백)       │
```

## 5. self-invocation — 프록시를 안 거친다

```
 ┌───────────────────────────────┐         ┌──────────────────────────┐
 │  PostService$$SpringCGLIB$$0   │ target  │  PostService  (진짜)       │
 │  b() {                        │ ──────▶ │  a() {                   │
 │     트랜잭션 시작;                │         │     this.b();    ★       │
 │     target.b();  ─────────────┼─────────┼──▶  }                    │
 │     커밋;                      │         │  b() { ... }             │
 │  }                            │         └──────────────────────────┘
 └───────────────────────────────┘
          ▲  @Autowired 로 받은 건 이것
```

```
 this  =  진짜 객체.  프록시가 아니다.
```

진짜 객체는 **자기를 감싼 프록시의 존재를 모른다.**

```
 [ 외부 ]  postService.b()     프록시.b() → 트랜잭션 ✅ → target.b()
 [ 내부 ]  a() 안에서 this.b()  진짜.b() 바로 실행 ❌  애노테이션을 아무도 안 읽는다
```

**프록시는 "문"이다.** 바깥에서 들어오려면 지나야 하지만, 이미 안에 있는 사람이
안에서 움직이는 건 문을 안 지난다.

`private` 메서드에 안 먹는 이유도 같다 — 상속으로 오버라이드하는데 private 은 불가.

### 해결

```java
otherService.b();              // 다른 빈 = 그 빈의 프록시를 거친다  ✅
@Autowired private PostService self;  self.b();   // 가능하지만 어색
```

### 과제 #3 에서 만날 모습

```java
public void 발급요청(Long memberId) { this.발급(memberId); }

@Transactional
public void 발급(Long memberId) { 재고를_줄인다(); 발급내역을_저장한다(); }
```

```
 재고를_줄인다()        ✅ 커밋  (각각 따로)
 발급내역을_저장한다()   ❌ 에러
       ↓
 재고는 줄었는데 발급 내역이 없다  →  쿠폰이 증발한다
```

**애노테이션을 빠뜨린 게 아니라, 붙였는데 무시된 것.** 코드만 보면 걸릴 것처럼 생겼다.

## 6. Controller 에 붙이면 안 되는 이유 (보완)

```
 [ Service ]                        [ Controller ]
  요청 파싱 / 검증                     ┌ 트랜잭션 시작
  ┌ 트랜잭션 시작                       │  요청 파싱 / 검증 / DB / JSON 변환 / 응답
  │  DB 조회 · 수정                     └ 커밋
  └ 커밋                                    ↑
  JSON 변환 / 응답                     커넥션을 그 내내 쥔다
```

```
 ① 커넥션 점유     트랜잭션이 열려 있는 동안 DB 커넥션을 쥔다.
                 HikariCP 기본 10개.  오래 쥘수록 동시 처리량이 준다
 ② 롤백 범위      JSON 직렬화 실패로 DB 가 롤백된다.  말이 안 된다
```

## 7. 로깅도 AOP

```
 @Transactional     트랜잭션
 @Async             별도 스레드
 @Cacheable         캐시
 @PreAuthorize      권한 체크
```

```java
@Aspect @Component
public class TimerAspect {
    @Around("execution(* com.board..*Service.*(..))")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();            // ← 진짜 메서드 실행
        log.info("{} {}ms", pjp.getSignature(), System.currentTimeMillis() - start);
        return result;
    }
}
```

이 한 번으로 모든 Service 메서드의 실행시간이 찍힌다. 메서드마다 `log.info` 를 쓸 필요가 없다.

---

## 다음에 물을 것

```
 □  프록시가 뭔지 스스로 설명할 수 있나      (이번엔 설명을 듣고 이해한 단계)
 □  self-invocation 을 그림 없이 말할 수 있나
 □  Controller 에 붙이면 안 되는 이유 — 커넥션 점유 / 롤백 범위
```
