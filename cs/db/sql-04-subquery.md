# SQL 4일차 — 서브쿼리

## 복습 (3일차)

**Q1. `WHERE` 와 `HAVING` 은 각각 무엇을 거르나?**
> where은 group으로 묶기 전에 조건을 거는 것임. Having은 그룹화를 전부 진행후
> 집계된 값에 대한 조건을 두는 것임

🟢 정확하다.

**Q2. `HAVING` 에 별칭을 쓰면 PostgreSQL 에서 에러가 나는 이유는?**
> 몰라

🔴 — 어제 설명했으나 안 남음. 복습 목록에 재등록.

```
 FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT
                             ↑         ↑         ↑
                        total 을     total 이    total 쓸 수 있다
                        쓰려는데     여기서       (이미 만들어졌으니까)
                        아직 없다    만들어진다
```

---

## 먼저 — 서브쿼리란

**쿼리 안에 들어간 쿼리.** 기준값을 먼저 구해서 비교할 때 쓴다.

```
 3일차까지     내가 아는 값과 비교        >= 300000
 오늘부터      쿼리로 구한 값과 비교      >= (SELECT AVG(...) FROM ...)
```

### 네 가지 자리

```sql
-- ① 스칼라 서브쿼리 — 값 하나.  비교 연산자 오른쪽
SELECT * FROM product
WHERE price > (SELECT AVG(price) FROM product);
                    └──── 하나의 값(124800)으로 줄어든다 ────┘

-- ② IN — 여러 값 중에 있나
SELECT * FROM orders
WHERE member_id IN (SELECT id FROM member WHERE city = '서울');
                         └──── 여러 행(1,3,5,9) ────┘

-- ③ EXISTS — 있기는 한가
SELECT * FROM member m
WHERE EXISTS (SELECT 1 FROM orders o WHERE o.member_id = m.id);
                           └─ 한 건이라도 있으면 참 ─┘

-- ④ FROM 절 서브쿼리 — 결과를 테이블처럼 쓴다
SELECT AVG(total) FROM (
    SELECT SUM(quantity * unit_price) AS total
    FROM order_item GROUP BY order_id
);
```

### 동작 단계 — 스칼라 서브쿼리

```
 SELECT * FROM product WHERE price > (SELECT AVG(price) FROM product)

 ① 안쪽 먼저 실행
    SELECT AVG(price) FROM product    →    124800

 ② 그 값으로 치환
    SELECT * FROM product WHERE price > 124800

 ③ 평범한 쿼리가 된다
```

**안쪽이 먼저 돌고, 결과가 값으로 바뀌어 들어간다.**

### `IN` vs `EXISTS`

```
 IN       "이 목록 안에 있나"       안쪽을 전부 구한 뒤 비교
 EXISTS   "한 건이라도 있나"        하나 찾으면 바로 멈춘다
```

```sql
-- 같은 결과, 다른 방식
WHERE id IN     (SELECT member_id FROM orders)
WHERE EXISTS    (SELECT 1 FROM orders o WHERE o.member_id = m.id)
```

`EXISTS` 안의 `SELECT 1` 은 **값을 안 쓴다**는 뜻이다. 존재 여부만 보므로 뭘 적든 상관없다.

### ⚠️ `NOT IN` 의 함정

```sql
WHERE id NOT IN (SELECT member_id FROM orders)
```

목록 안에 **`NULL`이 하나라도 있으면 결과가 통째로 비어버린다.** `NOT EXISTS` 는 안 그렇다.
이번 스키마는 `member_id`가 `NOT NULL`이라 괜찮지만, 습관은 `NOT EXISTS` 쪽이 안전하다.

### 상관 서브쿼리 (correlated)

안쪽이 바깥 행을 참조하면 **행마다 다시 실행**된다.

```sql
SELECT * FROM product p
WHERE price > (SELECT AVG(price) FROM product WHERE category = p.category);
                                                          └── 바깥 p 를 쓴다
```

```
 '전자' 행을 볼 때   →  전자 평균과 비교
 '도서' 행을 볼 때   →  도서 평균과 비교
```

**N+1 과 같은 구조다.** 바깥 행 수만큼 안쪽이 돈다.

---

## 직접 확인해볼 것

```sql
-- 안쪽만 따로 돌려보기
SELECT AVG(price) FROM product;

-- 스칼라 자리에 여러 행이 오면?
SELECT * FROM product WHERE price > (SELECT price FROM product);
```

---

## 2026-10-15 (4일차)

> 8문제. 막히면 안쪽 쿼리부터 따로 돌려볼 것.

### 문제 1. **평균 가격보다 비싼 상품**을 조회하라 (상품명, 가격, 비싼 순)

기대 결과 (**5행**)

**내 답**
```sql
select name, price
from product
where price > (select avg(p.price) from product p)
order by price DESC;
```
✅ 정답 (5행)


---

### 문제 2. **가장 비싼 상품**을 조회하라 (상품명, 가격)

> `ORDER BY ... LIMIT 1` 말고 **서브쿼리로** 풀 것. 동점이 있으면 둘 다 나와야 한다.

기대 결과 (**1행**)

**내 답**
```sql
select name, price
from product
where price = (selecr max(p.price) from product p);
```
⚠️ 논리는 정답. **`selecr` 오타**


---

### 문제 3. ⭐ **주문을 한 번도 하지 않은 회원**을 조회하라 (이름, 도시)

> 2일차에 `LEFT JOIN + IS NULL` 로 풀었던 문제다. 오늘은 **서브쿼리로** 풀어볼 것.

기대 결과 (**1행**)

**내 답**
```sql
select name, city
from member m
where NOT EXIST ( select 1 from orders o where m.id = o.member_id);
```
⚠️ 논리는 정답. **`EXIST` → `EXISTS`**

2일차엔 `LEFT JOIN + IS NULL` 로 푼 문제. 두 방식 다 가능하다.


---

### 문제 4. **서울에 사는 회원의 주문**을 조회하라 (주문 id, 주문일시)

> `JOIN` 말고 `IN` 으로.

기대 결과 (**9행**)

**내 답**
```sql
select o.id, o.ordered_at
from orders o
where o.member_id in (select m.id from member m where m.city = '서울`);
```
⚠️ 논리는 정답. **닫는 따옴표가 백틱(`` ` ``)**


---

### 문제 5. **주문한 적이 있는 회원**을 조회하라 (이름)

> `EXISTS` 를 써볼 것.

기대 결과 (**9행**)

**내 답**
```sql
select m.name
from member m
where EXIST (select 1 from orders o where m.id = o.member_id);
```
⚠️ 논리는 정답. **`EXIST` → `EXISTS`**


---

### 문제 6. **한 번도 팔리지 않은 상품**을 조회하라 (상품명, 카테고리)

기대 결과 (**2행**)

**내 답**
```sql
select p.name, p.category
from product p
where NOT EXIST (select 1 from order_item oi where p.id = oi.product_id);
```
⚠️ 논리는 정답. **`EXIST` → `EXISTS`**


---

### 문제 7. ⭐⭐ **자기 카테고리의 평균 가격보다 비싼 상품**을 조회하라 (상품명, 카테고리, 가격)

> 상관 서브쿼리. 전자 상품은 전자 평균과, 도서 상품은 도서 평균과 비교해야 한다.

기대 결과 (**6행**)

**내 답**
```sql
-- 처음 답 (오답)
where p.price > (
  select avg(p2.price) from product p2 group by p2.category);

-- 고친 답 ✅
where p.price > (
  select avg(p2.price) from product p2 where p2.category = p.category);
```
❌ → ✅ (6행)

**안쪽이 4행을 돌려주는 게 문제였다.**

```
 select avg(p2.price) from product p2 group by p2.category
   → 27666(도서) / 30633(식품) / 86725(의류) / 124800(전자)   4행
```

비교 연산자 오른쪽은 **값 하나**를 기대하는 자리다.
**SQLite 는 에러 없이 첫 행만 쓴다** → 도서 평균 27666 으로 전부 비교 → 12행.
PostgreSQL·MySQL 이었으면 `more than one row returned by a subquery` 에러가 난다.

```
 GROUP BY         나눠서 여러 줄로 내놓는다        ← 바깥 행과 연결이 없다
 상관 서브쿼리      바깥 행에 맞는 한 줄만 돌려준다   ← where p2.category = p.category
```

```
  바깥 행                안쪽이 받는 값         결과
 ──────────────────────────────────────────────────
  무선 이어폰 / 전자       category = '전자'     124800
  자바의 정석 / 도서       category = '도서'      27666
         ▲
   행마다 안쪽이 다시 실행된다 — N+1 과 같은 구조
```


---

### 문제 8. ⭐⭐⭐ **회원별 총 구매액이 전체 평균보다 많은 회원**을 조회하라 (이름, 총액, 많은 순)

> 오늘의 고비. **집계를 두 번** 해야 한다.
> ① 회원별 총 구매액을 구한다 → ② 그 값들의 평균을 구한다 → ③ ①에서 ②보다 큰 것만
> `FROM` 절 서브쿼리가 필요하다.

기대 결과 (**4행**). 1위는 `김동현 / 803900`

**내 답**
```sql
select name, total
from (select m.name, sum(unit_price * quantity) as total
      from member m
      join orders o ON o.member_id = m.id
      join order_item oi ON o.id = oi.order_id
      group by m.id, m.name)
where total > (
  select avg(total)
  from (select m.name, sum(unit_price * quantity) as total
        from member m
        join orders o ON o.member_id = m.id
        join order_item oi ON o.id = oi.order_id
        group by m.id, m.name))
ORDER BY total DESC;
```
✅ 정답 (4행, 1위 `김동현 / 803900`)

처음 제출엔 `as total` 이 빠져 `no such column: total` 이 났다. 구조는 한 번에 맞혔다.

**왜 HAVING 으로는 안 되나**

```
 HAVING 이 도는 시점      "김동현 바구니" 하나를 보고 있다
                        다른 회원의 합계는 아직 모른다
 필요한 것               9명 전부의 합계를 구한 뒤의 평균
```

```sql
avg(sum(...))   -- ❌ 집계 함수 안에 집계 함수
```

한 쿼리 안에서 집계를 두 번 할 수 없다. 그래서 **① 의 결과를 테이블처럼 감싼다.**


---

## 리뷰

```
 1 ✅   2 ⚠️   3 ⚠️   4 ⚠️   5 ⚠️   6 ⚠️   7 ❌→✅   8 ✅
        └──────── 오타 5건 ────────┘      논리 8/8
```

**논리는 전부 맞았다.** 7번도 상관 서브쿼리 개념을 짚어주자 바로 고쳤다.

### 오타 — 같은 패턴이 반복된다

```
 2일차   별칭·세미콜론·오타로 4문제 실패.  개념은 전부 맞음
 4일차   EXIST(×3), selecr, 백틱 → 5건.  논리는 전부 맞음
```

`EXISTS` 는 "존재하다(exist)" 가 아니라 **"존재한다(exists)" — 쿼리가 주어인 3인칭 단수**.
손 코딩 면접에서는 실행해보고 고칠 기회가 없다.

### 오늘 새로 안 것

1. 스칼라 자리에 여러 행이 오면 **SQLite 는 조용히 첫 행만 쓴다** (다른 DB 는 에러)
2. `GROUP BY` 와 상관 서브쿼리는 방향이 다르다 — 나누기 vs 바깥 행에 맞추기
3. 집계를 두 번 하려면 `FROM` 절 서브쿼리로 감싼다 (`avg(sum(...))` 은 불가)

## 배운 것

(내 말로)
