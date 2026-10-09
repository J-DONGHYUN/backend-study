# MVC 패턴 vs Spring MVC

> 참고용 정리. 읽고 나서 **내 말로 요약한 노트는 따로** 작성할 것.

---

## 0. 결론부터

**"MVC 패턴"과 "Spring MVC"는 이름만 같고 가리키는 범위가 다르다.**

| | MVC 패턴 | Spring MVC |
|---|---|---|
| 정체 | **디자인 패턴** (1970년대 Smalltalk) | Spring의 **웹 모듈 이름** |
| 핵심 | 화면(View)과 데이터(Model)를 분리 | **Front Controller 패턴** 기반 웹 프레임워크 |
| 범위 | 프레젠테이션 계층을 어떻게 나눌까 | 요청을 받아 처리하고 응답하는 전체 구조 |

**"DispatcherServlet이 받고 → HandlerMapping → Controller → 응답"** 은
**MVC 패턴의 정의가 아니라 Spring MVC의 요청 처리 흐름**이다. 정확히는 **Front Controller 패턴**.

---

## 1. 원래 MVC 패턴

```
         ① 입력
  사용자 ──────▶ ┌──────────────┐
                │  Controller  │  ② "이 작업 해줘" 위임
     ▲          │  (입력 처리)   │─────────┐
     │          └──────────────┘         ▼
     │                              ┌──────────┐
     │ ④ 화면                        │  Model   │  데이터 + 비즈니스 로직
     │                              └──────────┘
     │          ┌──────────────┐         │
     └───────── │     View     │◀────────┘
                │  (화면 생성)   │  ③ 상태 조회
                └──────────────┘
```

| 역할 | 책임 |
|---|---|
| **Model** | 데이터와 **비즈니스 로직**. 화면이 뭔지 모른다 |
| **View** | Model을 **보여주는 방법**. 로직이 없다 |
| **Controller** | 입력을 받아 **Model에 위임**하고, 결과를 View로 넘긴다 |

**핵심 목적:** 화면이 바뀌어도 비즈니스 로직은 안 바뀌고, 로직이 바뀌어도 화면은 안 바뀌게 한다.

> ⚠️ **Controller는 비즈니스 로직을 담지 않는다.** 입력을 받아 위임하는 역할이다.
> 로직이 Controller에 쌓이는 걸 **Fat Controller**라 부르고 안티패턴으로 본다.

---

## 2. 우리 Todo 프로젝트에 대입하면

```
 MVC               우리 프로젝트
───────────────────────────────────────────────────────────
 Controller   →    TodoController                      ✅ 있음
 Model        →    Todo(domain) + TodoService
                   + TodoRepository                    ✅ 있음
 View         →    ❌ 없음
                   대신 Jackson이 객체 → JSON 직렬화
```

**그래서 정확한 표현은 이렇다.**

> "Spring MVC 모듈 위에서 **View가 없는 REST API**를 만들었다."

`@RestController` = `@Controller` + `@ResponseBody` 이고,
`@ResponseBody`가 바로 **"View를 쓰지 말고 반환값을 응답 본문에 직접 써라"**는 뜻이다.

| | `@Controller` | `@RestController` |
|---|---|---|
| 반환값 해석 | **View 이름** | **응답 본문 데이터** |
| 처리 주체 | ViewResolver → JSP/Thymeleaf | HttpMessageConverter(Jackson) |
| 결과 | HTML | JSON |

---

## 3. Spring MVC의 정체 = Front Controller 패턴

### 이게 없으면

```
GET  /todos     ──▶ TodoListServlet    ┐
POST /todos     ──▶ TodoCreateServlet  │  서블릿마다
GET  /users     ──▶ UserListServlet    │  인증·로깅·파싱·예외처리를
POST /users     ──▶ UserCreateServlet  ┘  전부 중복 구현 💥
```

### Front Controller

```
   모든 요청  ──▶  ┌─────────────────────┐  ──▶  알맞은 Controller로 분배
                 │  DispatcherServlet  │
                 │  (입구가 하나)        │       공통 처리는 여기서 한 번만
                 └─────────────────────┘       (인증, 로깅, 파싱, 예외)
```

**입구를 하나로 모으는 것**이 Front Controller 패턴이고,
Spring에서 그 역할을 하는 게 **`DispatcherServlet`** 이다.

---

## 4. 요청 처리 전체 흐름

```
 ①  HTTP 요청   POST /todos  {"title":"운동하기"}
        │
        ▼
┌───────────────────────────────────────────────────────────────┐
│  DispatcherServlet                                            │
│                                                               │
│  ② HandlerMapping                                             │
│     "POST /todos 는 누가 처리하지?"                               │
│     → TodoController.create()  를 찾아냄                        │
│                                                               │
│  ③ HandlerAdapter — 찾은 핸들러를 실제로 호출한다                  │
│     ├─ ArgumentResolver                                       │
│     │    @PathVariable / @RequestParam 값을 꺼내 매개변수로        │
│     ├─ HttpMessageConverter (Jackson)                         │
│     │    @RequestBody : JSON → TodoCreateRequest              │
│     └─ @Valid 검증  (실패하면 여기서 예외 ──┐)                     │
│                             │              │                  │
│                             ▼              │                  │
│  ④  TodoController.create(request)         │                  │
│         └──▶ TodoService    비즈니스 로직, 트랜잭션 경계           │
│                  └──▶ TodoRepository ──▶ DB                   │
│                             │              │                  │
│  ⑤ 반환값 처리               ▼              │                  │
│     @RestController → HttpMessageConverter │                  │
│                       TodoResponse → JSON  │                  │
│     (@Controller였다면 → ViewResolver → HTML)                  │
└────────────────────────────────────────────┼──────────────────┘
        │                                    │
        ▼                                    │
 ⑥  201 Created + JSON                       │
                                             │
 ─────────────── 예외가 난 경우 ───────────────┘
        │
        ▼
   HandlerExceptionResolver 체인
        │
        ├─ 이 예외를 맡는 @ExceptionHandler 가 있나?
        │     있다 → GlobalExceptionHandler.handleNotFound()
        │             → 404 + ErrorResponse
        │
        └─ 없다 → 500 Internal Server Error
```

**`@RestControllerAdvice`가 끼어드는 자리가 바로 저 체인이다.**
새 `try-catch`를 만드는 게 아니라, **이미 있는 catch에 규칙을 등록**하는 것.

---

## 5. MVC vs 3계층 구조 — 다른 축이다

둘은 경쟁 관계가 아니다. **보는 방향이 다르다.**

```
          MVC (프레젠테이션을 어떻게 나눌까)
                      │
   ┌──────────────────┼──────────────────┐
   │                  │                  │
Controller          View               Model
   │                  │                  │
   ▼                  ▼                  ▼
┌──────────────────────────────────────────────┐
│  Controller 계층   TodoController            │  ← HTTP 변환
├──────────────────────────────────────────────┤
│  Service 계층      TodoService               │  ← 비즈니스 로직  ┐
├──────────────────────────────────────────────┤                 │ MVC의
│  Repository 계층   TodoRepository            │  ← 영속성        │ Model에
├──────────────────────────────────────────────┤                 │ 해당
│  Domain           Todo, BaseEntity           │  ← 도메인 규칙    ┘
└──────────────────────────────────────────────┘
          3계층 (애플리케이션 전체를 어떻게 나눌까)
```

**MVC의 "Model"을 3계층에서 Service / Repository / Domain으로 더 쪼갠 것**이라고 보면 된다.
우리 프로젝트는 **둘 다 쓰고 있다.**

---

## 6. 면접 답변 템플릿

**Q. 이 프로젝트는 Spring MVC로 만드셨나요?**
> "네. `spring-boot-starter-webmvc`를 사용했습니다.
> 다만 **View는 없습니다.** `@RestController`로 JSON을 반환하는 REST API라서,
> 전통적인 MVC의 View 자리를 **Jackson의 메시지 컨버터가 대신**합니다."

**Q. Spring MVC의 요청 처리 흐름을 설명해보세요.**
> "모든 요청은 **`DispatcherServlet`** 이 받습니다. **Front Controller 패턴**입니다.
> ① `HandlerMapping`이 URL과 HTTP method로 **어느 컨트롤러 메서드가 처리할지** 찾고,
> ② `HandlerAdapter`가 그 핸들러를 호출합니다. 이때 `ArgumentResolver`와
> `HttpMessageConverter`가 **JSON을 DTO로 변환**하고 `@Valid` 검증이 일어납니다.
> ③ 컨트롤러가 서비스에 위임해 로직을 처리하고,
> ④ 반환값은 `@ResponseBody`가 있으면 메시지 컨버터가 **JSON으로**,
> 없으면 `ViewResolver`가 **View를 찾아 렌더링**합니다.
> 중간에 예외가 나면 `HandlerExceptionResolver`로 빠지고,
> 거기에 `@RestControllerAdvice`가 등록한 핸들러가 동작합니다."

**Q. Front Controller 패턴을 왜 쓰나요?**
> "입구를 하나로 모아서 **인증, 로깅, 예외 처리 같은 공통 관심사를 한 곳에서** 처리하기 위해서입니다.
> 없으면 서블릿마다 같은 코드를 반복해야 합니다."

---

## 흔한 함정

| 흔한 오해 | 사실 |
|---|---|
| "MVC 패턴 = DispatcherServlet 흐름" | 그건 **Front Controller 패턴**. MVC는 Model/View/Controller 역할 분리 |
| "Controller에서 비즈니스 처리를 한다" | Controller는 **위임만** 한다. 로직은 Model(Service/Domain) |
| "REST API도 MVC니까 View가 있다" | **View는 없다.** 메시지 컨버터가 그 자리를 대신한다 |
| "MVC와 3계층은 같은 것" | **다른 축.** MVC의 Model을 3계층으로 더 쪼갠 관계 |

---

# 7. 교과서 흐름도를 우리 코드에 대입하기

## 7-1. 교과서 8단계 (`@Controller` + View 기준)

```
                    ┌──────────────┐          ┌──────────────────┐
                    │  핸들러 매핑   │          │ 핸들러 어댑터 목록  │
                    └──────────────┘          └──────────────────┘
                           ▲                          ▲
                    1.핸들러 조회              2.처리 가능한 어댑터 조회
                           │                          │
   HTTP 요청    ┌───────────────────────┐  3.handle()  ┌──────────────┐ 4.호출 ┌────────────┐
  ────────────▶│   DispatcherServlet   │─────────────▶│ 핸들러 어댑터  │──────▶│  컨트롤러   │
               │                       │◀─────────────│              │       └────────────┘
               └───────────────────────┘ 5.ModelAndView└──────────────┘
                    │           ▲
         8.render() │           │ 7.View 반환        6.viewResolver 호출
                    │           └──────────────┐   ┌──────────────┐
                    ▼                          └──▶│ viewResolver │
   HTML 응답 ◀──┌────────┐                         └──────────────┘
               │  View  │
               └────────┘
```

**중요: 5~8번은 `@Controller`(HTML 렌더링) 전용이다.**
우리는 `@RestController`라서 **5~8이 통째로 다른 길**로 간다.

## 7-2. 단계별 대입표

| # | 교과서 | 우리 코드 | `@RestController`에서는 |
|---|---|---|---|
| — | HTTP 요청 | `POST /todos` + JSON 본문 | 동일 |
| **1** | 핸들러 조회 | `RequestMappingHandlerMapping`이 `@PostMapping` + `/todos`를 보고 **`TodoController.create()`** 를 찾음 | **동일** |
| **2** | 핸들러 어댑터 조회 | 애노테이션 기반이므로 `RequestMappingHandlerAdapter`가 선택됨 | **동일** |
| **3** | `handle(handler)` | 어댑터가 호출 준비. **`ArgumentResolver`** 가 매개변수를 채운다 | **동일** |
| 3-a | (교과서엔 없음) | `@RequestBody` → **`HttpMessageConverter`(Jackson)** 가 JSON → `TodoCreateRequest` | **REST의 핵심** |
| 3-b | (교과서엔 없음) | `@Valid` → Bean Validation 수행. 실패 시 `MethodArgumentNotValidException` | **REST의 핵심** |
| **4** | 핸들러 호출 | `TodoController.create(request)` 실행 → `TodoService` → `TodoRepository` → DB | **동일** |
| **5** | `ModelAndView` 반환 | ❌ **우리는 `ResponseEntity<TodoResponse>` 반환** | `@ResponseBody`가 있으므로 **ModelAndView를 만들지 않음** |
| **6** | viewResolver 호출 | ❌ **없음** | `ReturnValueHandler`가 "본문에 직접 쓰기"로 분기 |
| **7** | View 반환 | ❌ **없음** | 〃 |
| **8** | `render(model)` | ❌ **없음** | `HttpMessageConverter`가 **객체 → JSON 직렬화** |
| — | HTML 응답 | **JSON 응답** (`201 Created`) | |

> **한 줄 요약:** 1~4는 똑같고, **5~8이 `HttpMessageConverter` 하나로 대체**된다.

---

## 7-3. 실제 요청 추적 — `POST /todos`

### ⓪ 클라이언트가 보낸 것

```http
POST /todos HTTP/1.1
Content-Type: application/json

{"title":"운동하기","description":"헬스장 가기"}
```

### ① 핸들러 조회 — `HandlerMapping`

```
"POST" + "/todos"  를 가진 핸들러를 찾아라
          │
          ▼
@RestController
@RequestMapping("/todos")      ← 클래스 레벨 경로
public class TodoController {
    @PostMapping               ← 메서드 레벨: POST + 추가 경로 없음
    public ResponseEntity<TodoResponse> create(...)   ★ 찾았다
```

### ② 핸들러 어댑터 조회

```
핸들러가 @RequestMapping 기반인가? → 예
   └▶ RequestMappingHandlerAdapter 선택
```

> 어댑터가 따로 있는 이유: 옛날 `Controller` 인터페이스, `HttpRequestHandler` 등
> **핸들러 종류가 여러 가지**여서, 각각을 호출하는 방법을 어댑터가 흡수한다.
> 덕분에 DispatcherServlet은 핸들러 종류를 몰라도 된다. (어댑터 패턴)

### ③ 매개변수 채우기 — `ArgumentResolver` + `HttpMessageConverter`

```java
public ResponseEntity<TodoResponse> create(@Valid @RequestBody TodoCreateRequest request)
                                            │       │
                                            │       └── ③-a MessageConverter
                                            │              JSON → 객체
                                            └────────── ③-b Bean Validation
```

**③-a** `MappingJackson2HttpMessageConverter`가 동작한다.

```
{"title":"운동하기","description":"헬스장 가기"}
                  │  Jackson 역직렬화
                  ▼
TodoCreateRequest[title="운동하기", description="헬스장 가기"]
```

```java
public record TodoCreateRequest(
    @NotBlank(message = "제목은 필수")
    @Size(max = 50, message = "제목은 50자 이하여야합니다.")
    String title,
    String description
) { }
```

**③-b** `@Valid`가 위 제약을 검사한다.

```
title = "운동하기"  → @NotBlank ✓  @Size(max=50) ✓   통과 → ④로

title = "   "     → @NotBlank ✗   MethodArgumentNotValidException
                                   → 예외 경로(아래 7-4)로 빠짐
```

### ④ 핸들러(컨트롤러) 호출 → 서비스 → DB

```java
// TodoController — HTTP 변환만. 로직 없음
TodoResponse response = todoService.create(request);
```
```java
// TodoService — 비즈니스 로직 + 트랜잭션 경계
@Transactional
public TodoResponse create(TodoCreateRequest request) {
    Todo todo  = Todo.create(request.title(), request.description());  // DTO → Entity
    Todo saved = todoRepository.save(todo);                            // INSERT
    return TodoResponse.from(saved);                                   // Entity → DTO
}
```

```
TodoCreateRequest          Todo (Entity)              TodoResponse (DTO)
┌──────────────────┐      ┌──────────────────┐      ┌────────────────────┐
│ title            │      │ id        = 1    │      │ id        = 1      │
│ description      │─────▶│ title            │─────▶│ title              │
└──────────────────┘      │ description      │      │ description        │
   Todo.create()          │ completed = false│      │ completed = false  │
                          │ createdAt ← @PrePersist │ createdAt          │
                          │ updatedAt ←      │      │ updatedAt          │
                          └──────────────────┘      └────────────────────┘
                             save() 시점에             TodoResponse.from()
                             INSERT + 시각 자동 기록
```

**Entity는 여기서 멈춘다.** 컨트롤러로 나가는 건 `TodoResponse`뿐이다.

### ⑤~⑧ 응답 — View가 아니라 MessageConverter

```java
return ResponseEntity.status(HttpStatus.CREATED).body(response);
```

```
ResponseEntity<TodoResponse>
        │
        ▼
 ReturnValueHandler : "@ResponseBody가 있네 → View 안 쓴다"
        │                (@RestController = @Controller + @ResponseBody)
        ▼
 HttpMessageConverter (Jackson) : 객체 → JSON 직렬화
        │
        ▼
HTTP/1.1 201 Created
Content-Type: application/json

{"id":1,"title":"운동하기","description":"헬스장 가기","completed":false,
 "createdAt":"2026-10-07T16:47:02.737199","updatedAt":"2026-10-07T16:47:02.737199"}
```

**ViewResolver도 View도 호출되지 않는다.** 교과서 6·7·8번이 통째로 생략된다.

---

## 7-4. 예외가 났을 때 (교과서 그림에 없는 경로)

```
④ 컨트롤러/서비스 실행 중  💥 TodoNotFoundException
        │
        ▼
 DispatcherServlet의 catch 블록
        │
        ▼
 HandlerExceptionResolver 체인
        │
        ├─ ExceptionHandlerExceptionResolver
        │     "이 예외를 맡는 @ExceptionHandler가 있나?"
        │        있다 ──▶ GlobalExceptionHandler.handleNotFound()
        │                    │
        │                    ▼  ErrorResponse 객체
        │              HttpMessageConverter → JSON
        │                    │
        │                    ▼
        │              404 {"status":404,"message":"Todo를 찾을 수 없음 id = 999",...}
        │
        └─ 없다 ──▶ 500 Internal Server Error
```

**우리 프로젝트의 매핑**

| 예외 | 발생 지점 | 핸들러 | 응답 |
|---|---|---|---|
| `MethodArgumentNotValidException` | ③-b `@Valid` | `handleValidation` | 400 |
| `TodoNotFoundException` | ④ Service의 `orElseThrow` | `handleNotFound` | 404 |
| `TodoAlreadyCompletedException` | ④ Entity의 `complete()` | `handleAlreadyCompleted` | 409 |

> `@Valid` 실패는 **컨트롤러에 들어가기도 전(③단계)에** 터진다. 이게 중요한 포인트.

---

## 7-5. 면접에서 말하는 대본

> "요청이 오면 **`DispatcherServlet`** 이 먼저 받습니다. Front Controller 패턴입니다.
>
> **첫째, `HandlerMapping`** 이 URL과 HTTP 메서드를 보고 어떤 컨트롤러 메서드가 처리할지 찾습니다.
> 제 프로젝트에서는 `POST /todos` 가 `TodoController.create()` 로 매핑됩니다.
>
> **둘째, `HandlerAdapter`** 를 찾습니다. 핸들러 종류가 여러 가지라서, 호출 방법을 어댑터가 흡수합니다.
>
> **셋째, 어댑터가 핸들러를 호출하기 전에 매개변수를 채웁니다.**
> `@RequestBody`가 붙어 있으면 **`HttpMessageConverter`가 JSON을 DTO로 역직렬화**하고,
> `@Valid`가 있으면 **이 시점에 Bean Validation이 돌아갑니다.**
> 그래서 검증 실패는 **컨트롤러에 들어가기도 전에** 터집니다.
>
> **넷째, 컨트롤러가 실행**됩니다. 제 컨트롤러는 서비스에 위임만 하고 로직이 없습니다.
> 서비스가 트랜잭션 경계를 잡고, 엔티티를 만들어 저장한 뒤 **응답 DTO로 변환해서** 반환합니다.
> 엔티티는 서비스 밖으로 나가지 않습니다.
>
> **다섯째부터가 갈립니다.** 교과서 그림에서는 `ModelAndView`를 반환하고
> `ViewResolver`가 View를 찾아 `render()` 하는데,
> **제 프로젝트는 `@RestController`라 그 경로를 타지 않습니다.**
> `@ResponseBody`가 붙은 셈이라, **`HttpMessageConverter`가 객체를 JSON으로 직렬화해서
> 응답 본문에 바로 씁니다.** 결과는 `201 Created` 와 JSON입니다.
>
> 중간에 예외가 나면 `HandlerExceptionResolver` 로 빠지는데,
> 제가 만든 `@RestControllerAdvice` 가 거기 등록되어 있어서
> 404 · 409 · 400 으로 매핑하고 **일관된 에러 JSON**을 내보냅니다."

### 꼬리 질문 대비
- **"HandlerAdapter는 왜 필요한가요?"** → 핸들러 종류(애노테이션 방식, 옛 `Controller` 인터페이스 등)가 여럿이라, 호출 방식을 어댑터가 흡수한다. DispatcherServlet은 종류를 몰라도 된다.
- **"`@Controller`와 `@RestController` 차이는?"** → `@RestController` = `@Controller` + `@ResponseBody`. 반환값을 View 이름이 아니라 **응답 본문**으로 해석한다.
- **"`@Valid` 검증은 언제 일어나나요?"** → 컨트롤러 호출 **전**, `HandlerAdapter`가 매개변수를 바인딩하는 단계.
- **"JSON을 객체로 누가 바꾸나요?"** → `HttpMessageConverter`. Spring Boot에서는 `MappingJackson2HttpMessageConverter`가 기본.
