# 쓸 거리

> 학습하다 "아 이거였구나" 하는 순간이 오면 **그 자리에서 한 줄만** 적는다.
> 글은 나중에 쓴다. 적어두지 않으면 일주일 뒤에 사라진다.

## 대기

- [ ] **수정했는데 updatedAt이 한 박자 밀려 있었다** — 그리고 테스트 19개가 못 잡은 이유
      → `assignments/2026-10-06-todo-crud/` · `2026-10-08-board/interview-log.md`
      → 포인트: null은 티가 나지만 "그럴듯한 옛날 날짜"는 안 보인다

- [ ] **N+1을 쿼리 로그로 직접 세어봤다 (12개 → 3개)**
      → `assignments/2026-10-08-board/README.md` 쿼리 수 측정 기록
      → 포인트: N은 "부모 건수"다. 댓글이 300개여도 쿼리 수는 같다

- [ ] **LEFT JOIN + COUNT(*) 의 함정** — 0이어야 할 게 1로 나온다
      → `cs/db/sql-02-join.md`
      → 아주 짧게 쓸 수 있다

- [ ] **@ExceptionHandler(Exception.class) 를 만들었더니 400이 500이 됐다**
      → `assignments/2026-10-08-board/README.md` 구현 메모 11번

## 발행됨 — jdh-devlog.tistory.com

| 날짜 | 카테고리 | 제목 |
|---|---|---|
| 2026-10-14 | spring | JPA 쓰기 지연 — 저장했는데 id가 null |
| 2026-10-14 | spring | JPA랑 Hibernate가 뭐가 다른가요? — JDBC부터 Spring Data JPA까지 |
| 2026-10-14 | 네트워크 | TCP 3-way handshake를 tcpdump로 직접 봤습니다 |
| 2026-10-14 | 네트워크 | TCP vs UDP — 같은 컴퓨터 안인데 패킷 740개가 사라졌습니다 |
| 2026-10-14 | 네트워크 | HTTP vs HTTPS |
