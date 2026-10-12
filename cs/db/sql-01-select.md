# SQL 01 — SELECT / WHERE / ORDER BY / LIMIT

> 답을 먼저 보지 말 것. 쓰고 → 실행하고 → 결과를 확인한다.

```bash
cd ~/Desktop/study/cs/db
sqlite3 shop.db
.headers on
.mode column
```

---

## 2026-10-10 (1일차)

### 문제 1. 전체 회원의 이름·등급·도시를 조회하라

기대 결과 (10행)
```
name  grade   city
----  ------  ----
김동현  GOLD    서울
이서연  SILVER  부산
...
```

**내 답**
```sql
select name, grade, city from member;
```

---

### 문제 2. **서울**에 사는 회원만 조회하라 (이름, 등급)

기대 결과 (4행)

**내 답**
```sql
select name, grade from member where city = '서울';
```

---

### 문제 3. 가격이 **50,000원 이상**인 상품을 **비싼 순**으로 조회하라 (상품명, 카테고리, 가격)

기대 결과 (6행). 첫 행은 `27인치 모니터 / 전자 / 329000`

**내 답**
```sql
select name, category, price from product where price > 50000 ORDER BY DESC;   -- ❌ 에러
```

---

### 문제 4. **품절**(재고 0)인 상품을 조회하라

기대 결과 (2행)

**내 답**
```sql
select * from product where stock = 0;
```

---

### 문제 5. **가장 비싼 상품 3개**를 조회하라 (상품명, 가격)

기대 결과 (3행)

**내 답**
```sql
select name, price from product order by desc limit 3;   -- ❌ 에러
```

---

### 문제 6. **2025년에 가입한** 회원을 **가입일 순**으로 조회하라 (이름, 가입일)

기대 결과 (5행). 첫 행은 `정하윤 / 2025-01-10`

> 힌트: `joined_at` 은 `'2025-01-10'` 같은 문자열이다. 날짜 함수를 안 써도 푸는 방법이 있다.

**내 답**
```sql
select name, joined_at from member where joined_at like '2025%'
```

---

### 문제 7. **전자** 카테고리 중 **10만원 미만**인 상품을 **싼 순**으로 조회하라

기대 결과 (3행)

**내 답**
```sql
select * from product where category = '전자' and price < 100000 orderby asc;   -- ❌ 에러
```

---

### 문제 8. 이름에 **'코드'** 가 들어간 상품을 찾아라

기대 결과 (1행)

> 힌트: 부분 일치를 찾는 연산자가 있다.

**내 답**
```sql
select * from product where name like '%코드%';
```

---

## 리뷰 — 5/8

| # | 결과 |
|---|---|
| 1, 2, 4, 6, 8 | ✅ |
| **3, 5, 7** | ❌ **전부 같은 원인: `ORDER BY` 문법** |

### 🔴 `ORDER BY` — "무엇을 기준으로" 가 빠졌다

```sql
ORDER BY DESC              ❌  "내림차순으로 정렬해"  → 뭘?
ORDER BY price DESC        ✅  "price 를 기준으로 내림차순"
```

에러 메시지가 그대로 말해준다.
```
Error: no such column: DESC      ← DB: "DESC 라는 컬럼을 찾는 건가요?"
```

```sql
ORDER BY 컬럼명 [ASC | DESC]
         ━━━━━  ━━━━━━━━━━
         필수     생략 가능 (기본 ASC)
```

### 🔴 `orderby` 는 붙여 쓰면 안 된다
```sql
orderby asc     ❌
ORDER BY price  ✅    -- ORDER BY, GROUP BY 는 두 단어다
```

### 🟡 `>` 와 `>=`
문제는 "50,000원 **이상**" 인데 `>`(초과)를 썼다.
이번엔 정확히 50,000원인 상품이 없어 결과가 같았을 뿐이다.

| 한국어 | 연산자 |
|---|---|
| **이상** | `>=` |
| **초과** | `>` |
| **이하** | `<=` |
| **미만** | `<` |

> 면접 SQL 문제는 한국어로 나온다. **"이상/초과" 를 잘못 읽어 틀리는 경우가 많다.**

### 🟡 `SELECT *` 보다 필요한 컬럼만
- 필요 없는 컬럼까지 네트워크로 전송된다
- **컬럼이 추가되면 결과가 조용히 바뀐다**
- 인덱스만 읽고 끝낼 수 있는 쿼리가 테이블까지 읽게 된다

### 💡 6번은 잘 풀었다 — 다만 더 나은 방법이 있다
```sql
WHERE joined_at LIKE '2025%'                                    -- 내 답
WHERE joined_at >= '2025-01-01' AND joined_at < '2026-01-01'    -- 더 나음
```
**`LIKE '2025%'` 는 인덱스를 못 탈 수 있지만 범위 조건은 탄다.**
지금은 10행이라 차이가 없지만 100만 행이면 완전히 다르다. → `sql-06-index.md`

### 고친 쿼리
```sql
-- 3번
SELECT name, category, price FROM product
WHERE price >= 50000
ORDER BY price DESC;

-- 5번
SELECT name, price FROM product
ORDER BY price DESC
LIMIT 3;

-- 7번
SELECT name, price FROM product
WHERE category = '전자' AND price < 100000
ORDER BY price;
```

## 배운 것

(내 말로 작성할 것)
