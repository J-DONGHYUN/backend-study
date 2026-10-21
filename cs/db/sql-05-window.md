# 윈도우 함수 (참고용 — 진행 안 함)

> **2026-10-21 보류.** 신입 공고의 "간단한 쿼리 손 코딩" 범위를 넘는다.
> 경력·데이터 직군에서나 나온다. 그 시간에 **인덱스/트랜잭션/락**을 하는 게 맞다는 판단.
> 나중에 필요해지면 아래 개념 정리부터 보면 된다. 문제는 안 푼 상태로 둔다.

## 복습 (구두)

**Q1. `HAVING` 에 별칭을 쓰면 PostgreSQL 에서 에러가 나는 이유는?**
> (세 번째 질문. 10/13, 10/16 두 번 다 못 답함)

**Q2. "한 건이라도 있나" 를 묻는 키워드의 철자는?**
> (10/16 에 `EXIST` 로 3번 씀)

---

## 먼저 — 윈도우 함수란

**`GROUP BY` 는 행을 합치고, 윈도우 함수는 행을 남긴다.**

```
 원본 15행
      │
      ├─ GROUP BY category    →   4행    (행이 줄어든다. 개별 상품이 사라진다)
      │
      └─ 윈도우 함수            →  15행    (행은 그대로. 옆에 계산 결과가 붙는다)
```

```
 [ GROUP BY ]                      [ 윈도우 함수 ]

 category   avg(price)             name          category   price    avg
 ────────────────────────          ──────────────────────────────────────
 도서        27666                 무선 이어폰     전자       129000   124800
 식품        30633                 기계식 키보드    전자        89000   124800
 의류        86725                 27인치 모니터   전자       329000   124800
 전자       124800                 면 티셔츠       의류        19900    86725
                                   ...                       (15행 그대로)
 4행                                                          15행
```

**"개별 행과 집계값을 같이 보고 싶을 때"** 쓴다.

### 기본 문법

```sql
함수() OVER (PARTITION BY 묶을기준 ORDER BY 정렬기준)
            └──────────── 윈도우(창) 정의 ────────────┘
```

```
 PARTITION BY   어떻게 나눌 것인가    (GROUP BY 와 비슷하지만 행을 안 합친다)
 ORDER BY       창 안에서의 순서      (순위 함수에 필요)
 둘 다 생략 가능  생략하면 전체가 창 하나
```

### 순위 함수 세 가지

```sql
ROW_NUMBER() OVER (ORDER BY price DESC)
RANK()       OVER (ORDER BY price DESC)
DENSE_RANK() OVER (ORDER BY price DESC)
```

```
 price    ROW_NUMBER   RANK   DENSE_RANK
 ──────────────────────────────────────────
 100          1          1        1
  90          2          2        2
  90          3          2        2      ← 동점
  80          4          4        3      ← RANK 는 3을 건너뛴다
```

```
 ROW_NUMBER   무조건 1,2,3,4      동점이어도 순서를 매긴다
 RANK         동점은 같은 등수,   다음 등수를 건너뛴다  (올림픽식)
 DENSE_RANK   동점은 같은 등수,   다음 등수를 안 건너뛴다
```

### 집계 함수도 OVER 를 붙일 수 있다

```sql
SELECT name, category, price,
       AVG(price) OVER (PARTITION BY category) AS cat_avg
FROM product;
```

**4일차 7번(상관 서브쿼리)과 같은 일을 한다.** 더 짧고 빠르다.

```sql
-- 4일차에 쓴 방법
WHERE price > (SELECT AVG(p2.price) FROM product p2 WHERE p2.category = p.category)

-- 윈도우 함수
AVG(price) OVER (PARTITION BY category)
```

### ⚠️ WHERE 에서 못 쓴다

```sql
SELECT ... WHERE ROW_NUMBER() OVER (...) = 1      -- ❌ 에러
```

**실행 순서 때문이다.** 윈도우 함수는 `SELECT` 단계에서 계산되는데 `WHERE` 는 그보다 먼저다.
그래서 **서브쿼리로 감싼 뒤 바깥에서 거른다.**

```sql
SELECT * FROM (
    SELECT ..., ROW_NUMBER() OVER (...) AS rk FROM ...
) WHERE rk = 1;
```

> 4일차 8번에서 쓴 `FROM` 절 서브쿼리와 같은 구조다.

---

## 직접 확인해볼 것

```sql
-- GROUP BY 와 나란히 비교
SELECT category, AVG(price) FROM product GROUP BY category;
SELECT name, category, price, AVG(price) OVER (PARTITION BY category) FROM product;

-- WHERE 에서 쓰면 어떤 에러가 나는지
SELECT * FROM product WHERE ROW_NUMBER() OVER (ORDER BY price) = 1;
```

---

## 2026-10-21 (5일차)

### 문제 1. 전체 상품에 **비싼 순으로 순위**를 매겨 조회하라 (상품명, 가격, 순위)

기대 결과 (**15행**). 1위는 `27인치 모니터 / 329000`

**내 답**
```sql

```

---

### 문제 2. **카테고리별로** 비싼 순 순위를 매겨라 (카테고리, 상품명, 가격, 순위)

> 카테고리마다 1위부터 다시 시작해야 한다.

기대 결과 (**15행**)

**내 답**
```sql

```

---

### 문제 3. ⭐ **카테고리별 가장 비싼 상품**을 조회하라 (카테고리, 상품명, 가격)

> 2번에서 1위만 남기면 된다. `WHERE` 에 윈도우 함수를 바로 못 쓴다는 걸 기억할 것.

기대 결과 (**4행**)

```
 도서   클린 코드        33000
 식품   유기농 꿀        38000
 의류   패딩 점퍼       189000
 전자   27인치 모니터   329000
```

**내 답**
```sql

```

---

### 문제 4. ⭐ **회원별 첫 주문**을 조회하라 (회원 id, 주문 id, 주문일시)

> 주문이 없는 회원은 빠진다.

기대 결과 (**9행**)

**내 답**
```sql

```

---

## 리뷰

(Claude 피드백)

## 배운 것

(내 말로)
