# SQL 02 — JOIN ⭐

> **면접 SQL 문제의 절반 이상이 JOIN 이다.** 여기가 본론.
> 답을 먼저 보지 말 것. 쓰고 → 실행하고 → **행 수를 비교**한다.

```bash
cd ~/Desktop/study/cs/db
sqlite3 shop.db
.headers on
.mode box
```

---

## 먼저 — 기본 문법

```sql
SELECT  m.name, o.id
FROM    member m                       -- 기준 테이블 + 별칭(alias)
JOIN    orders o  ON m.id = o.member_id   -- 어떤 컬럼으로 이을지
WHERE   o.status = 'PAID';
```

| | |
|---|---|
| **별칭** `member m` | 길어지면 필수. `m.name` 처럼 쓴다 |
| **`ON`** | **어떤 컬럼으로 이을지.** 빠뜨리면 모든 조합이 나온다 (카티전 곱) |
| **`m.name`** | 같은 이름 컬럼이 양쪽에 있으면 **어느 테이블인지 명시** |

### INNER vs LEFT

```
   member            orders
   ┌────┐           ┌────┐
   │ 1  │───────────│ 1  │
   │ 2  │───────────│ 2  │
   │ 3  │           └────┘
   └────┘
     ▲ 3번 회원은 주문이 없다

 INNER JOIN →  1, 2 만        (양쪽에 다 있는 것만)
 LEFT  JOIN →  1, 2, 3        (3번은 orders 컬럼이 NULL)
```

**`JOIN` 은 `INNER JOIN` 의 줄임말이다.**

---

## 2026-10-12 (2일차)

### 문제 1. 모든 주문의 **주문 id · 주문일 · 주문한 회원 이름**을 조회하라

기대 결과 (**20행**)

**내 답**
```sql
select o.id, o.ordered_at, m.name
from orders o
INNER join member m ON m.id = o.member_id;
```

---

### 문제 2. 상태가 **`PAID`** 인 주문의 **주문 id · 회원 이름 · 주문일**을 조회하라

기대 결과 (**13행**)

**내 답**
```sql
select o.id, m.name, o.order_at          -- ❌ 오타: ordered_at
from member m
INNER join orders o ON m.id = o.member_id
where o.status = 'PAID';
```

---

### 문제 3. **주문한 적이 있는** 회원의 이름을 조회하라 (중복 없이)

기대 결과 (**9행**)

> 힌트: 그냥 JOIN 하면 주문 수만큼 이름이 반복된다. 중복을 없애는 키워드가 있다.

**내 답**
```sql
select distinct m.name
from member m
INNER join orders o ON m.id = o.member_id
```

---

### 문제 4. ⭐ **주문이 한 번도 없는** 회원을 찾아라

기대 결과 (**1행**)

> 힌트: `INNER JOIN` 으로는 절대 못 찾는다. 주문이 없으니 이어질 게 없다.
> `LEFT JOIN` 으로 **일단 다 가져온 뒤**, 오른쪽이 비어 있는 행만 남기면 된다.
> "비어 있다" 는 `= NULL` 이 아니라 **다른 문법**을 쓴다.

**내 답**
```sql
select m.*
from member m
left join orders o On m.id = o.member_id
where o.memeber id IS NULL;              -- ❌ 오타: member_id
```

---

### 문제 5. ⭐ **한 번도 팔리지 않은** 상품을 찾아라 (상품명, 카테고리)

기대 결과 (**2행**)

> 4번과 완전히 같은 패턴이다.

**내 답**
```sql
select p.name, p.category
from product                              -- ❌ 별칭 p 누락
left join order_item item on p.id = item.product_id
where item.id is NULL;
```

---

### 문제 6. 주문 상세에 **상품명**을 붙여 조회하라 (주문상세 id, 상품명, 수량, 주문당시 단가)

기대 결과 (**40행**)

**내 답**
```sql
select ot.id, p.name, ot.quantity, ot.unit_price
from order_item ot
left join product p on ot.product_id = p.id;    -- ⚠️ INNER 가 맞다
```

---

### 문제 7. ⭐ **어떤 회원이 어떤 상품을 샀는지** 조회하라 (회원명, 주문일, 상품명, 수량)

> **네 테이블을 전부 이어야 한다.** 이 패턴이 면접에 가장 많이 나온다.
> ```
> member ──< orders ──< order_item >── product
> ```

기대 결과 (**40행**)

**내 답**
```sql
select m.name, o.ordered_at, p.name, ot.quantity
from member m
left join orders o ON m.id = o.member_id        -- ⚠️ 41행! INNER 여야 40행
left join order_item ot ON o.id = ot.order_id
left join product p ON p.id = ot.product_id;
```

---

### 문제 8. **'김동현'** 이 구매한 상품 목록을 조회하라 (상품명, 수량, 주문일)

기대 결과 (**10행**)

**내 답**
```sql
select p.name, ot.quantity, o.ordered_at
from member m
left join orders o ON m.id = o.member_id
left join order_item ot ON o.id = ot.order_id
left join product p ON p.id = ot.product_id;    -- ❌ 세미콜론 위치
where m.name = '김동현';
```

---

## 리뷰 — 개념은 전부 맞았다. 오타·문법만 틀림

| # | 결과 | 원인 |
|---|---|---|
| 1 | ✅ 20행 | |
| 2 | ❌ | **오타** (`order_at` → `ordered_at`) |
| 3 | ✅ 9행 | |
| 4 | ❌ | **오타 2개** (`memeber`, `member id`) — 구조는 정확 |
| 5 | ❌ | **별칭 누락** (`from product` → `from product p`) |
| 6 | ✅ 40행 | ⚠️ `INNER` 가 의미상 맞다 |
| **7** | ⚠️ **41행** | **`LEFT` 때문에 주문 없는 회원이 끼어듦.** 기대는 40행 |
| 8 | ❌ | **세미콜론 위치** |

**`LEFT JOIN + IS NULL` 패턴도, 4테이블 사슬도 구조는 정확히 썼다.**
1일차의 `ORDER BY` 같은 개념 오류는 없었다.

---

### ⭐ 7번 — 왜 41행인가

```
                   member    +orders   +order_item   +product
 ─────────────────────────────────────────────────────────────
 LEFT JOIN           10    →    21    →     41      →    41
                                 ▲
                      주문 없는 회원(한수아)이 NULL 로 계속 따라온다

 INNER JOIN          10    →    20    →     40      →    40
                                 ▲
                           여기서 탈락하고 끝
```

**문제는 "어떤 회원이 어떤 상품을 샀는지"** 다. 한수아는 아무것도 안 샀으니 결과에 있으면 안 된다.

### JOIN 은 "곱해지는" 연산이다
```
 member 1행 ──┬── 주문 0건  →  INNER: 0행(사라짐) / LEFT: 1행(NULL)
              ├── 주문 1건  →  1행
              └── 주문 3건  →  3행   ← 한 사람이 3줄이 된다
```
임시우(1명) × 주문(2건) × 상품(3개) = **3행**.
→ 이게 **GROUP BY 가 필요한 이유**다. 3줄을 다시 1줄로 묶어야 한다.

---

### ⭐ `LEFT JOIN` 의 두 가지 용도

**① "XX 없는 것" 찾기** — `LEFT JOIN ... WHERE 오른쪽 IS NULL`
> "주문 안 한 회원", "댓글 없는 게시글", "로그인한 적 없는 사용자"

**② "0도 포함해 전부 보여주기"** — 실무에서 더 흔하다
```
 INNER:  주문 있는 회원 9명만
 LEFT :  전원 10명. 한수아는 0
```
> "회원별 주문 수", "게시글별 댓글 수(0 포함)", "상품별 판매량"
> **과제 #2 의 `commentCount` 가 정확히 이것이다.**

**③ 선택적(optional) 관계** — 외래키가 NULL 허용이면 대개 `LEFT`

### 판단 기준
> **"오른쪽에 짝이 없는 왼쪽 행을, 결과에 포함할 것인가?"**

| 질문 | JOIN |
|---|---|
| "뭘 샀나" | 안 산 사람은 답이 아니다 → `INNER` |
| "안 산 사람은?" | 안 산 사람이 답이다 → `LEFT` + `IS NULL` |
| "사람별 구매 수" | 0명도 목록에 있어야 한다 → `LEFT` + `COUNT(컬럼)` |

---

### ⚠️ 면접 단골 함정 — `LEFT JOIN` + `COUNT(*)`

```
┌──────┬────────────┬─────────────┐
│ name │ COUNT(*)   │ COUNT(o.id) │
├──────┼────────────┼─────────────┤
│ 임시우  │ 2          │ 2           │   같다
│ 한수아  │ 1  💥      │ 0  ✅       │   다르다!
└──────┴────────────┴─────────────┘
```
`LEFT JOIN` 후 한수아의 행은 **값이 NULL 일 뿐 행은 1개 존재**한다.
- `COUNT(*)`    → 행을 센다 → **1**
- `COUNT(o.id)` → **NULL 은 세지 않는다** → **0** ✅

**`LEFT JOIN` + `COUNT` 는 반드시 `COUNT(오른쪽테이블.컬럼)`.**

---

### 💡 `IS NULL` 을 잘 썼다
```sql
WHERE o.member_id IS NULL     ✅
WHERE o.member_id = NULL      ❌ 아무것도 안 나온다
```
`NULL` 은 "값이 없음" 이라 `=` 로 비교가 안 된다. `NULL = NULL` 조차 참이 아니다.

---

### 고친 쿼리
```sql
-- 2번
SELECT o.id, m.name, o.ordered_at
FROM member m
INNER JOIN orders o ON m.id = o.member_id
WHERE o.status = 'PAID';

-- 4번
SELECT m.name
FROM member m
LEFT JOIN orders o ON m.id = o.member_id
WHERE o.member_id IS NULL;

-- 5번
SELECT p.name, p.category
FROM product p                              -- 별칭 선언
LEFT JOIN order_item item ON p.id = item.product_id
WHERE item.id IS NULL;

-- 7번  (LEFT → INNER)
SELECT m.name, o.ordered_at, p.name, ot.quantity
FROM member m
JOIN orders o      ON m.id = o.member_id
JOIN order_item ot ON o.id = ot.order_id
JOIN product p     ON p.id = ot.product_id;

-- 8번  (세미콜론은 맨 끝에만)
SELECT p.name, ot.quantity, o.ordered_at
FROM member m
JOIN orders o      ON m.id = o.member_id
JOIN order_item ot ON o.id = ot.order_id
JOIN product p     ON p.id = ot.product_id
WHERE m.name = '김동현';
```

## 배운 것

(내 말로 작성할 것)
