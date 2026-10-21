# DTO 와 Entity 분리

## 구술 기록

### 2026-10-21 (3일차) — 🟡 유지

**Q1. Entity 를 API 응답으로 그대로 내보내면 안 되는 이유는?**

> entity에는 보안적으로 감춰야하는 정보가 있을 수 있기 때문에 Entity 자체를 API로 응답을
> 보내면 안된다.

**Q2. 당신은 어떻게 하셨나요?**

> Response DTO 만들어 필요한 정보만 넣어서 객체를 생성 후 응답합니다. 실제 외부로 나갈때는
> RestController 에노테이션을 통해 객체를 json으로 바꿔서 보내줍니다

**꼬리. `content` 필드명을 `body` 로 바꾸면 프론트에 무슨 일이?**

> 프론트에서는 서버가 갑작스럽게 컬럼명을 바꾼다면 반환받았을 때 이 컬럼명이 어떤 컬럼인지
> 모릅니다

### 채점

```
 ✅  민감정보 노출
 △   API 스펙 ↔ DB 스키마 결합   유도하니 문턱까지는 옴.  스스로는 안 나옴
 ❌  순환 참조 (양방향 연관관계 → Jackson 무한루프)
```

**10/08 과 같은 답.** 13일 지났는데 민감정보에서 멈추는 건 그대로다.

### 결합이 더 중요한 이유

```
 [ Entity 직접 반환 ]                [ DTO 를 거칠 때 ]

  DB 컬럼  content → body            DB 컬럼  content → body
       │                                  │
       ▼                             PostResponse 안에서
  응답 JSON 도 body 로 바뀜            body 를 content 로 담아준다
       │                                  │
       ▼                                  ▼
  프론트 깨짐  ❌                    응답 JSON 은 그대로  ✅
```

```javascript
<p>{ post.content }</p>      →   undefined
```

```
 DTO 가 하는 일  =  API 스펙과 DB 스키마 사이의 완충지대
```

```
 민감정보      @JsonIgnore 로도 막을 수 있다        대안이 있다
 결합          DTO 말고는 끊을 방법이 없다           대안이 없다   ★
```

면접관이 민감정보 답을 듣고 **"그건 @JsonIgnore 로 되지 않나요?"** 라고 되묻는 게 전형적인 전개다.

### 또 나온 실수 — 주어가 빠진다

```
 "RestController 애노테이션을 통해 객체를 json으로 바꿔서 보내줍니다"
```

`@RestController` 는 JSON 으로 바꾸는 게 아니라 **MessageConverter 를 쓰도록 지시**한다.
바꾸는 건 Jackson.

**2026-10-13 Spring MVC 구술에서도 똑같은 자리에서 주어가 빠졌다.** 두 번째다.
