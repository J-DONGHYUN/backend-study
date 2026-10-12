# SQL 연습

매일 15~20분. **손으로 쿼리를 쓰는 것**이 목적이다.

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
