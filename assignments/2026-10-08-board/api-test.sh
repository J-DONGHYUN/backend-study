#!/bin/bash
# 게시판 API 테스트 스크립트
#
# H2가 인메모리라 앱을 끄면 데이터가 전부 사라진다.
# 앱을 새로 띄울 때마다 seed 를 돌려서 데이터를 다시 만든다.
#
# 사용법:
#   ./api-test.sh seed     게시글 10개 + 각 댓글 3개 (총 댓글 30개) 생성
#   ./api-test.sh check    기본 동작 검증
#   ./api-test.sh errors   에러 케이스 확인
#   ./api-test.sh nplus1   N+1 측정용: seed 후 목록 조회 1번만 호출

BASE=http://localhost:8080

wait_for_app() {
  for i in $(seq 1 60); do
    curl -s -o /dev/null "$BASE/posts" 2>/dev/null && return 0
    sleep 1
  done
  echo "앱이 응답하지 않습니다. ./gradlew bootRun 으로 먼저 띄우세요."; exit 1
}

seed() {
  echo "게시글 10개 + 각 댓글 3개 생성 중..."
  for p in $(seq 1 10); do
    id=$(curl -s -X POST "$BASE/posts" -H "Content-Type: application/json" \
      -d "{\"title\":\"게시글 $p\",\"content\":\"본문 $p 입니다\",\"author\":\"작성자$p\"}" \
      | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
    for c in 1 2 3; do
      curl -s -o /dev/null -X POST "$BASE/posts/$id/comments" -H "Content-Type: application/json" \
        -d "{\"content\":\"게시글 $p 의 댓글 $c\",\"author\":\"댓글러$c\"}"
    done
  done
  echo "완료: 게시글 10개, 댓글 30개"
}

check() {
  echo "### 1. 게시글 등록 (201) ###"
  curl -s -w "\n  status=%{http_code}\n" -X POST "$BASE/posts" -H "Content-Type: application/json" \
    -d '{"title":"검증용 글","content":"본문","author":"tester"}'

  echo "### 2. 댓글 등록 (201, id/createdAt 이 null 이 아니어야 함) ###"
  curl -s -w "\n  status=%{http_code}\n" -X POST "$BASE/posts/1/comments" -H "Content-Type: application/json" \
    -d '{"content":"검증용 댓글","author":"tester"}'

  echo "### 3. 단건 조회 (200, comments 포함) ###"
  curl -s -w "\n  status=%{http_code}\n" "$BASE/posts/1"

  echo "### 4. 목록 조회 (200, 최신순 + commentCount) ###"
  curl -s -w "\n  status=%{http_code}\n" "$BASE/posts?page=0&size=5"

  echo "### 5. 수정 (200, updatedAt 이 createdAt 보다 늦어야 함) ###"
  curl -s -w "\n  status=%{http_code}\n" -X PUT "$BASE/posts/1" -H "Content-Type: application/json" \
    -d '{"title":"수정된 제목","content":"수정된 본문"}'

  echo "### 6. 댓글 삭제 (204) ###"
  curl -s -o /dev/null -w "  status=%{http_code}\n" -X DELETE "$BASE/posts/1/comments/1"

  echo "### 7. 게시글 삭제 (204, 댓글도 함께 삭제) ###"
  curl -s -o /dev/null -w "  status=%{http_code}\n" -X DELETE "$BASE/posts/1"
}

errors() {
  echo "### 없는 게시글 조회 (404 기대) ###"
  curl -s -w "\n  status=%{http_code}\n" "$BASE/posts/999999"

  echo "### 없는 게시글에 댓글 (404 기대) ###"
  curl -s -w "\n  status=%{http_code}\n" -X POST "$BASE/posts/999999/comments" \
    -H "Content-Type: application/json" -d '{"content":"c","author":"a"}'

  echo "### 다른 게시글의 댓글 삭제 (404 기대) ###"
  curl -s -w "\n  status=%{http_code}\n" -X DELETE "$BASE/posts/2/comments/1"

  echo "### 제목 공백 (400 기대) ###"
  curl -s -w "\n  status=%{http_code}\n" -X POST "$BASE/posts" -H "Content-Type: application/json" \
    -d '{"title":"   ","content":"본문","author":"작성자"}'

  echo "### 제목 100자 초과 (400 기대) ###"
  LONG=$(printf 'a%.0s' {1..101})
  curl -s -w "\n  status=%{http_code}\n" -X POST "$BASE/posts" -H "Content-Type: application/json" \
    -d "{\"title\":\"$LONG\",\"content\":\"본문\",\"author\":\"작성자\"}"
}

nplus1() {
  seed
  echo ""
  echo ">>> 지금부터 앱 로그를 보세요. 아래 요청 하나가 쿼리를 몇 개 날리는지 셉니다."
  echo ">>> GET /posts?page=0&size=10"
  curl -s -o /dev/null "$BASE/posts?page=0&size=10"
  echo ">>> 요청 완료. 로그의 'Session Metrics' 에서 JDBC statements 수를 확인하세요."
}

wait_for_app
case "$1" in
  seed)   seed ;;
  check)  check ;;
  errors) errors ;;
  nplus1) nplus1 ;;
  *)      echo "사용법: ./api-test.sh [seed|check|errors|nplus1]" ;;
esac
