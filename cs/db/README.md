# SQL 연습

**주 2회 · 4문제 · 30분 이내.** 손으로 쿼리를 쓰는 것이 목적이다.

## 문법 학습은 끝났다 (1~4일차)

```
 1일차   SELECT / WHERE / ORDER BY / LIMIT     5/8   🟡
 2일차   JOIN                                  4/8   🟡
 3일차   GROUP BY / 집계 / HAVING              7/8   🟢
 4일차   서브쿼리 (스칼라·IN·EXISTS·FROM절)      8/8   🟢
```

신입 공고의 "간단한 쿼리 손 코딩"은 여기까지로 커버된다.
**윈도우 함수는 하지 않는다** — 경력·데이터 직군에서나 나온다. (`sql-05-window.md` 는 참고용으로만 남겨둠)

## 앞으로의 목적 — 감 유지 + 오타 줄이기

```
 논리    🟢 수준      개념은 거의 틀리지 않는다
 손      🔴 수준      오타가 반복된다
```

```
 2일차   오타·별칭·세미콜론            4건
 4일차   EXIST ×3,  selecr,  백틱     5건
 5일차   EXIST  (복습 문항에서)        1건
```

세 번 연속이다. 손 코딩 면접은 **실행해보고 고칠 기회가 없다.**

### 채점 기준이 바뀐다

```
 지금까지    오타는 ⚠️ 로 넘어갔다.  논리가 맞으면 정답
 앞으로      그대로 실행되지 않으면 오답       ★
```

### 출제 규칙

```
 ▸ 새 문법은 내지 않는다
 ▸ 1~4일차 범위를 섞은 복합 문제만        JOIN + GROUP BY + 서브쿼리
 ▸ 4문제 중 1개는 지난 회차에 틀린 유형을 변형해서 낸다
 ▸ 인덱스를 배운 뒤에는 "이 쿼리가 인덱스를 탈까?" 유형을 섞는다
```

### 반복 실수 체크리스트 (제출 전에 본다)

```
 □  EXISTS   — EXIST 아님
 □  세미콜론
 □  따옴표    — 백틱(`) 아님
 □  ORDER BY 에 정렬 기준 컬럼을 적었나
 □  COUNT(*) 인가 COUNT(컬럼) 인가 — LEFT JOIN 이면 특히
 □  HAVING 에 별칭 말고 식을 그대로 썼나
```

---

---

## 시작하기

```bash
cd ~/Desktop/study/cs/db
sqlite3 shop.db
```

처음이거나 데이터를 초기화하고 싶으면:
```bash
rm -f shop.db && sqlite3 shop.db < _schema_sqlite.sql
```

### 출력을 보기 좋게 (접속 후 한 번)
```sql
.headers on
.mode column
```

### 자주 쓰는 명령
```
.tables              테이블 목록
.schema member       테이블 구조 보기
.quit                나가기
```

### 터미널에서 한 줄로 실행
```bash
sqlite3 shop.db -header -column "SELECT * FROM member LIMIT 3;"
```

---

## 스키마

```
  member                     orders                    order_item              product
  ┌──────────────┐          ┌──────────────┐         ┌──────────────┐        ┌──────────────┐
  │ id           │◀────┐    │ id           │◀───┐    │ id           │   ┌───▶│ id           │
  │ name         │     └────│ member_id    │    └────│ order_id     │   │    │ name         │
  │ email        │          │ ordered_at   │         │ product_id   │───┘    │ category     │
  │ grade        │          │ status       │         │ quantity     │        │ price        │
  │ city         │          └──────────────┘         │ unit_price   │        │ stock        │
  │ joined_at    │                                   └──────────────┘        └──────────────┘
  └──────────────┘
     회원 10명                 주문 20건                주문상세 40건            상품 15개
```

| 컬럼 | 값 |
|---|---|
| `member.grade` | `BRONZE` / `SILVER` / `GOLD` |
| `member.city` | 서울 / 부산 / 대구 / 인천 / 대전 / 광주 |
| `product.category` | 전자 / 의류 / 식품 / 도서 |
| `orders.status` | `PAID` / `SHIPPED` / `CANCELED` |
| `order_item.unit_price` | **주문 시점 가격.** `product.price`(현재가)와 다를 수 있다 |

> **일부러 넣어둔 것들** — 실무 데이터는 깔끔하지 않다
> - **주문이 한 번도 없는 회원**이 있다 → `LEFT JOIN` 연습용
> - **한 번도 안 팔린 상품**이 있다 → 〃
> - **재고 0인 상품**이 있다
> - **취소된 주문**이 있다 → 집계에서 제외해야 할 때가 있다

---

## 커리큘럼

| 파일 | 주제 | 면접 빈도 |
|---|---|---|
| `sql-01-select.md` | `SELECT` / `WHERE` / `ORDER BY` / `LIMIT` | 기초 |
| `sql-02-join.md` | **`JOIN`** (INNER / LEFT / 셀프) | ⭐ 가장 많이 나옴 |
| `sql-03-groupby.md` | `GROUP BY` / `HAVING` / 집계함수 | ⭐ |
| `sql-04-subquery.md` | 서브쿼리 / `EXISTS` vs `IN` | ⭐ |
| `sql-05-window.md` | 윈도우 함수 (`ROW_NUMBER`, `RANK`) | 중급 |
| `sql-06-index.md` | 인덱스 / 실행계획 | ⭐ 차별화 지점 |
| `sql-07-tx.md` | 트랜잭션 / 격리 수준 | 과제 #3과 연결 |

---

## 진행 방식

```
 ① 문제와 "기대 결과" 를 받는다
 ② 직접 SQL 을 쓴다           ← 손코딩
 ③ 실행해서 결과를 비교한다
 ④ 리뷰 — 다른 풀이, 성능, 실행계획
```

**답을 먼저 보지 않는다.** 틀려도 일단 써보고 실행해서 확인한다.

---

## MySQL 로 바꾸고 싶다면

sqlite 는 설정이 0이라 매일 하기 좋지만, 채용공고는 보통 MySQL 을 쓴다.
**트랜잭션·격리 수준·락 실습(sql-07)** 은 MySQL 이 필요하다.

```bash
# Docker Desktop 을 먼저 실행한 뒤
docker run -d --name sql-lab -e MYSQL_ROOT_PASSWORD=labpass \
  -e MYSQL_DATABASE=shop -p 3307:3306 mysql:8

docker exec -i sql-lab mysql -uroot -plabpass shop < _schema.sql
docker exec -it sql-lab mysql -uroot -plabpass shop
```

> `_schema.sql`(MySQL용)과 `_schema_sqlite.sql`(sqlite용) 두 벌이 있다.

### 문법 차이 (연습하며 마주칠 것들)

| | sqlite | MySQL |
|---|---|---|
| 테이블 목록 | `.tables` | `SHOW TABLES;` |
| 구조 보기 | `.schema t` | `DESC t;` |
| 문자열 결합 | `\|\|` | `CONCAT()` |
| 날짜 포맷 | `strftime('%Y', d)` | `DATE_FORMAT(d,'%Y')` |
| 실행계획 | `EXPLAIN QUERY PLAN` | `EXPLAIN` |

**기본 `SELECT`/`JOIN`/`GROUP BY`/서브쿼리/윈도우 함수는 거의 같다.**
MySQL 전용 문법이 필요한 문제는 그때 따로 표시한다.
