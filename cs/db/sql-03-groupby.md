# SQL 03 — GROUP BY / 집계 / HAVING

> **2일차에서 "임시우가 3줄이 된 것" 을 1줄로 묶는 작업이다.**
> 답을 먼저 보지 말 것.

```bash
cd ~/Desktop/study/cs/db
sqlite3 shop.db
.headers on
.mode box
```

---

## 복습 (2일차)

쓰기 전에 먼저 답해보기.

1. `LEFT JOIN` 과 `INNER JOIN` 중 **어느 쪽을 쓸지 판단하는 기준**은?
2. `LEFT JOIN` 에서 `COUNT(*)` 를 쓰면 왜 틀리나?

---

## 먼저 — 문법

### 집계 함수

| | |
|---|---|
| `COUNT(*)` | **행**의 개수 |
| `COUNT(컬럼)` | 그 컬럼이 **`NULL` 이 아닌** 개수 |
| `SUM(컬럼)` | 합계 |
| `AVG(컬럼)` | 평균 |
| `MAX` / `MIN` | 최대 / 최소 |

> 외우기: **`*` 는 행, 컬럼은 값.**

---

## GROUP BY 는 어떻게 동작하나 — 단계별

### 1단계. 원본 테이블 (15행)

```
┌────┬──────────────┬──────────┬────────┐
│ id │     name     │ category │ price  │
├────┼──────────────┼──────────┼────────┤
│ 13 │ 자바의 정석      │ 도서      │  30000 │
│ 14 │ 클린 코드        │ 도서      │  33000 │
│ 15 │ 객체지향의 사실   │ 도서      │  20000 │
│ 10 │ 아메리카노 원두   │ 식품      │  24000 │
│ 11 │ 유기농 꿀        │ 식품      │  38000 │
│ 12 │ 견과류 세트      │ 식품      │  29900 │
│  6 │ 면 티셔츠        │ 의류      │  19900 │
│  7 │ 청바지          │ 의류      │  59000 │
│  8 │ 패딩 점퍼        │ 의류      │ 189000 │
│  9 │ 운동화          │ 의류      │  79000 │
│  1 │ 무선 이어폰      │ 전자      │ 129000 │
│  2 │ 기계식 키보드     │ 전자      │  89000 │
│  3 │ 27인치 모니터    │ 전자      │ 329000 │
│  4 │ USB-C 허브     │ 전자      │  45000 │
│  5 │ 노트북 스탠드     │ 전자      │  32000 │
└────┴──────────────┴──────────┴────────┘
```

### 2단계. `GROUP BY category` — **바구니로 나눈다**

```
 ┌─ 도서 바구니 ──────────────────┐   ┌─ 식품 바구니 ──────────────────┐
 │ 13  자바의 정석       30000    │   │ 10  아메리카노 원두    24000    │
 │ 14  클린 코드         33000    │   │ 11  유기농 꿀         38000    │
 │ 15  객체지향의 사실    20000    │   │ 12  견과류 세트       29900    │
 └───────────────────────────────┘   └───────────────────────────────┘

 ┌─ 의류 바구니 ──────────────────┐   ┌─ 전자 바구니 ──────────────────┐
 │  6  면 티셔츠         19900    │   │  1  무선 이어폰      129000    │
 │  7  청바지           59000    │   │  2  기계식 키보드     89000    │
 │  8  패딩 점퍼        189000    │   │  3  27인치 모니터   329000    │
 │  9  운동화           79000    │   │  4  USB-C 허브      45000    │
 └───────────────────────────────┘   │  5  노트북 스탠드     32000    │
                                     └───────────────────────────────┘
```

**이 시점에는 아직 "표" 가 아니다.** 행들이 바구니에 담겨 있을 뿐이다.

### 3단계. 집계 함수가 **각 바구니를 하나의 값으로** 압축

```
 ┌─ 전자 바구니 ───────┐
 │ 129000             │
 │  89000             │        COUNT(*)  → 5         "몇 개 들었나"
 │ 329000             │  ──▶   SUM(price)→ 624000    "다 더하면"
 │  45000             │        AVG(price)→ 124800    "평균은"
 │  32000             │        MAX(price)→ 329000    "제일 큰 건"
 └────────────────────┘
       5개 행                       바구니 하나당 값 하나
```

**바구니 하나가 결과의 한 줄이 된다.**

### 4단계. 최종 결과 (4행)

```
┌──────────┬────┬────────┬──────────┬────────┐
│ category │ 개수│  합계   │   평균    │  최고가  │
├──────────┼────┼────────┼──────────┼────────┤
│ 도서      │  3 │  83000 │  27666.6 │  33000 │
│ 식품      │  3 │  91900 │  30633.3 │  38000 │
│ 의류      │  4 │ 346900 │  86725.0 │ 189000 │
│ 전자      │  5 │ 624000 │ 124800.0│ 329000 │
└──────────┴────┴────────┴──────────┴────────┘
     ▲  바구니 4개 → 4행
```

---

## 💡 `SELECT` 에 아무 컬럼이나 못 쓰는 이유

```sql
SELECT name, category, COUNT(*)      -- ❌
FROM product GROUP BY category;
```

```
 ┌─ 전자 바구니 ───────┐
 │ 무선 이어폰          │
 │ 기계식 키보드        │   ← "전자 그룹의 name 은 뭐죠?"
 │ 27인치 모니터        │      5개 중에 뭘 골라야 하나?
 │ USB-C 허브         │
 │ 노트북 스탠드        │
 └────────────────────┘
```

**바구니 안에 값이 5개인데 한 줄에 하나만 넣어야 한다.** 고를 수가 없다.

```
 SELECT 에 올 수 있는 것
   ① 묶는 기준 (category)     → 바구니마다 하나뿐이니 확실하다
   ② 집계 함수 (COUNT, SUM)   → 여러 개를 하나로 압축하니 확실하다
```

---

## ⭐ WHERE vs HAVING — "행을 거르나, 바구니를 거르나"

```
 WHERE   ──▶  바구니에 "담기 전" 에 행을 거른다
 HAVING  ──▶  바구니를 "만든 후" 에 바구니를 거른다
```

### 눈으로 보면

```
 [WHERE price >= 50000]  — 담기 전에 거른다

 ┌─ 전자 바구니 ───────┐
 │ 129000  ✅          │
 │  89000  ✅          │     45000, 32000 은 애초에 안 담긴다
 │ 329000  ✅          │
 │  45000  ❌ 제외      │  →  COUNT(*) = 3
 │  32000  ❌ 제외      │
 └────────────────────┘


 [HAVING COUNT(*) >= 4]  — 바구니를 통째로 거른다

 ┌─ 도서 (3개) ┐  ❌ 바구니 삭제
 ┌─ 식품 (3개) ┐  ❌ 바구니 삭제
 ┌─ 의류 (4개) ┐  ✅ 통과
 ┌─ 전자 (5개) ┐  ✅ 통과
                       →  결과 2행
```

**`HAVING` 은 바구니 단위로 통째로 버린다.** 바구니 안의 행을 골라내는 게 아니다.

### 왜 `WHERE` 에 `COUNT(*)` 를 못 쓰나

```
 WHERE 가 실행될 때는 아직 바구니가 없다
      └─ 바구니가 없으니 "개수" 라는 것도 없다
           → COUNT(*) 를 계산할 대상이 없다 → 에러
```

---

## 실행 순서 한 장

```
  ①  FROM / JOIN      테이블을 가져온다
        ▼
  ②  WHERE            개별 행을 거른다            ← 집계 함수 못 씀
        ▼
  ③  GROUP BY         바구니로 나눈다
        ▼
  ④  HAVING           바구니를 거른다             ← 집계 함수 씀
        ▼
  ⑤  SELECT           각 바구니를 한 줄로 만든다
        ▼
  ⑥  ORDER BY         줄을 정렬한다
        ▼
  ⑦  LIMIT            몇 줄만 남긴다
```

**`WHERE` 가 `GROUP BY` 보다 먼저** 라는 것만 기억하면 `HAVING` 은 자동으로 풀린다.

> 💡 **성능 팁:** 거를 수 있으면 **`WHERE` 에서 미리 거르는 게 좋다.**
> 바구니에 담을 행 자체가 줄어든다. `HAVING` 으로만 거르면 일단 다 담았다가 버리는 셈이다.

### 직접 확인해볼 것

```sql
-- 바구니에 뭐가 들었는지 보기
SELECT category, COUNT(*), SUM(price), AVG(price), MAX(price)
FROM product GROUP BY category;

-- HAVING 으로 바구니 거르기  → 2행만 남는다
SELECT category, COUNT(*) FROM product
GROUP BY category HAVING COUNT(*) >= 4;

-- WHERE 에 COUNT 를 쓰면?  (에러를 한 번 볼 것)
SELECT category, COUNT(*) FROM product
WHERE COUNT(*) >= 4 GROUP BY category;
```

**마지막 쿼리의 에러를 한 번 보면** `WHERE` 와 `HAVING` 을 평생 안 헷갈린다.

---

## 2026-10-13 (3일차)

### 문제 1. **카테고리별 상품 개수**를 조회하라

기대 결과 (**4행**)

**내 답**
```sql
select category, count(*)
from product
group by category
```
✅ 정답 (4행)

---

### 문제 2. **카테고리별 평균 가격**을 조회하라 (비싼 순)

기대 결과 (**4행**)

**내 답**
```sql
select category, avg(*)
from product
group by category
order by avg(*) DESC
```
❌ **오답 — 실행 에러**

```
Error: wrong number of arguments to function avg()
```

`*` 를 받을 수 있는 집계 함수는 **`COUNT` 하나뿐**이다.

```
 COUNT(*)  ✅  "행을 세라"            → 값이 뭐든 상관없다
 AVG(*)    ❌  "평균을 내라" ...뭘?   → 계산할 컬럼이 있어야 한다
 SUM(*)    ❌
 MAX(*)    ❌
```

```
 [전자 바구니]  price: 129000, 89000, 329000, 45000, 32000
                        ↓
                 AVG(price) → 124800     "이 5개를 더해서 5로 나눠"
                 AVG(*)     → ???        "뭘 더해?"
```

고친 답
```sql
select category, avg(price) as avg_price
from product
group by category
order by avg_price DESC;
```


---

### 문제 3. ⭐ **회원별 주문 건수**를 조회하라. **주문이 0건인 회원도 0으로 표시**되어야 한다

기대 결과 (**10행**). 한수아가 `0` 으로 나와야 한다.

> 어제 본 함정이 여기 있다. `COUNT(*)` 를 쓰면 한수아가 `1` 로 나온다.

**내 답**
```sql
select m.id, count(o.id)
from member m
left join orders o  ON m.id = o.member_id
group by m.id
```
✅ 정답 (10행, 한수아 `0`)

`LEFT JOIN` + `COUNT(o.id)` — 어제 틀렸던 함정을 정확히 피했다.


---

### 문제 4. **주문별 총 금액**을 조회하라 (주문 id, 총액)

> 총액 = `quantity × unit_price` 의 합

기대 결과 (**20행**)

**내 답**
```sql
select o.id, sum(quantity*unit_price)
from order_item oi
join orders o ON oi.order_id = o.id
group by o.id
```
✅ 정답 (20행) — **다만 JOIN 하나가 불필요하다**

`order_item` 에 이미 `order_id` 컬럼이 있다. `o.id` 와 `oi.order_id` 는 같은 값이므로
`orders` 를 붙여서 얻은 게 아무것도 없다.

```sql
select order_id, sum(quantity * unit_price)
from order_item
group by order_id;
```

**JOIN 은 "저쪽 테이블에만 있는 걸 가져올 때" 한다.**
8번에서는 `orders.status` 가 필요하니 그때는 진짜로 JOIN 해야 한다.


---

### 문제 5. **회원별 총 구매액**을 조회하라 (많이 쓴 순)

기대 결과 (**9행**). 1위는 `김동현 / 803900`

> (취소 주문도 포함한 금액이다. 취소를 빼는 건 8번에서 한다.)

**내 답**
```sql
select m.name, sum(quantity*unit_price) as total
from member m
join orders o ON m.id = o.member_id
join order_item oi ON o.id = oi.order_id
group by m.name, m.id
order by total DESC
```
✅ 정답 (9행, 1위 `김동현 / 803900`)

`member → orders → order_item` 3단 JOIN 후 묶기. 주문 없는 한수아는 `INNER JOIN` 이라 빠졌다 — 문제가 요구한 그대로.


---

### 문제 6. **주문을 2건 이상** 한 회원을 조회하라 (이름, 주문수)

기대 결과 (**6행**)

> 힌트: "2건 이상" 은 **묶은 다음에야** 알 수 있는 조건이다.

**내 답**
```sql
select m.name, count(*)
from member m
join orders o ON m.id = o.member_id
group by m.id, m.name
having count(*) >= 2;
```
✅ 정답 (6행)

**여기선 `COUNT(*)` 가 왜 안전했나** — 어제 3번에서는 함정이었는데.

```
 3번   LEFT JOIN  →  한수아 행이 NULL 로 남는다  →  COUNT(*) 가 그 NULL 행도 센다  ✗
 6번   INNER JOIN →  주문 없는 회원은 아예 사라진다 →  남은 행은 전부 진짜 주문    ✅
```

**`COUNT(*)` 가 위험한 건 `LEFT JOIN` 과 만났을 때뿐이다.**

`group by m.id, m.name` — `m.id` 가 PK라 `m.name` 은 없어도 되지만, 넣어두는 쪽이 표준에 안전하다.
MySQL `ONLY_FULL_GROUP_BY` 모드에서는 빼면 에러가 난다.


---

### 문제 7. **가장 많이 팔린 상품 5개**를 조회하라 (상품명, 판매수량)

기대 결과 (**5행**). 1위는 `면 티셔츠 / 11`

**내 답**
```sql
select p.name, sum(quantity)
from order_item oi
join product p ON p.id = oi.product_id
group by oi.product_id, p.name
order by sum(quantity) DESC LIMIT 5;
```
✅ 정답 (5행, 1위 `면 티셔츠 / 11`)

상품명이 필요하니 `product` JOIN 이 진짜로 필요했다 (4번과 대비된다).


---

### 문제 8. ⭐ **취소된 주문을 제외**하고, **총 구매액이 30만원 이상**인 회원을 조회하라 (많이 쓴 순)

기대 결과 (**4행**). 1위는 `최지우 / 739000`

> **`WHERE` 와 `HAVING` 을 둘 다 써야 한다.**
> - 취소 주문 제외 → 개별 행 조건 → ?
> - 총액 30만 이상 → 집계 결과 조건 → ?

**내 답**
```sql
select m.name, sum(quantity*unit_price) as total
from member m
join orders o ON m.id = o.member_id
join order_item oi ON o.id = oi.order_id
where status != 'CANCELED'
group by m.id, m.name
having total >= 300000
order by total DESC;
```
✅ 정답 (4행, 1위 `최지우 / 739000`)

오늘의 핵심. `WHERE` / `HAVING` 배치가 정확하다.

```
 WHERE  status != 'CANCELED'   →  JOIN 된 개별 행을 거른다   (묶기 전)
 HAVING total >= 300000        →  묶어서 합친 값을 거른다    (묶은 후)
```

**⚠ 다만 `HAVING total` 은 SQLite·MySQL 에서만 통한다**

`total` 은 `SELECT` 에서 만든 별칭인데, 실행 순서를 보면

```
 FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY
                             ↑         ↑
                        여기서 total 을   여기서 total 이
                        쓰고 있는데       비로소 생긴다
```

`HAVING` 이 돌 때 `total` 은 아직 없다. SQLite·MySQL 은 편의로 봐주지만
**PostgreSQL·Oracle 은 에러**를 낸다.

```sql
having sum(quantity * unit_price) >= 300000   -- 어디서나 동작
order by total DESC                           -- 별칭 OK (SELECT 다음이라)
```

`ORDER BY` 에서 별칭이 되는 이유도 같은 그림으로 설명된다.

그리고 `where status != 'CANCELED'` → `where o.status != 'CANCELED'`.
지금은 `status` 가 `orders` 에만 있어 괜찮지만, 다른 테이블에 같은 이름이 생기면 그 순간 모호해진다.


---

## 리뷰

```
 1 ✅   2 ❌   3 ✅   4 ✅△   5 ✅   6 ✅   7 ✅   8 ✅
                        └ 불필요한 JOIN
```

**틀린 건 2번 하나.** `AVG(*)` 는 존재하지 않는다 — `*` 는 `COUNT` 전용이다.

오늘 새로 알게 된 세 가지
1. `*` 를 받는 집계 함수는 `COUNT` 뿐
2. `COUNT(*)` 가 위험한 건 **`LEFT JOIN` 과 만났을 때뿐**
3. `HAVING` 에서는 별칭을 쓰지 말 것 — `ORDER BY` 는 써도 됨 (실행 순서 때문)

## 배운 것

(내 말로)
