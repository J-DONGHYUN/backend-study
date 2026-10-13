# 쓸 거리

> 학습하다 "아 이거였구나" 하는 순간이 오면 **그 자리에서 한 줄만** 적는다.
> 글은 나중에 쓴다. 적어두지 않으면 일주일 뒤에 사라진다.

## 대기

- [ ] **수정했는데 updatedAt이 한 박자 밀려 있었다** — 그리고 테스트 19개가 못 잡은 이유
      → `assignments/2026-10-06-todo-crud/` · `2026-10-08-board/interview-log.md`
      → 포인트: null은 티가 나지만 "그럴듯한 옛날 날짜"는 안 보인다

- [ ] **HTTP로 로그인하면 비밀번호가 이렇게 보입니다** (tcpdump -A)
      → `cs/network/http-https.md` 7부
      → 포인트: HTTPS로 바꾸면 안 보이는 것까지 같은 화면으로 비교

- [ ] **N+1을 쿼리 로그로 직접 세어봤다 (12개 → 3개)**
      → `assignments/2026-10-08-board/README.md` 쿼리 수 측정 기록
      → 포인트: N은 "부모 건수"다. 댓글이 300개여도 쿼리 수는 같다

- [ ] **3-way handshake를 눈으로 보기** (tcpdump + netstat)
      → `cs/network/tcp-basics.md`
      → 포인트: accept()가 반환될 땐 이미 handshake가 끝나 있다

- [ ] **같은 컴퓨터 안인데 UDP 패킷이 2.5% 사라졌다**
      → `cs/network/lab/UdpLossTest.java`
      → 포인트: 네트워크 문제가 아니라 수신 버퍼가 넘친 것. 흐름 제어가 없어서

- [ ] **LEFT JOIN + COUNT(*) 의 함정** — 0이어야 할 게 1로 나온다
      → `cs/db/sql-02-join.md`
      → 아주 짧게 쓸 수 있다

- [ ] **@ExceptionHandler(Exception.class) 를 만들었더니 400이 500이 됐다**
      → `assignments/2026-10-08-board/README.md` 구현 메모 11번

## 발행됨

(발행하면 여기로 옮기고 URL 적기)
