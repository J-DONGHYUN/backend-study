# [실습 3·4] HTTP 와 HTTPS

> 개념부터. 읽고 나서 **내 말로 요약한 노트는 따로** 작성할 것.

---

# 1부. TCP 와 HTTP 는 무슨 관계인가

## 한 줄

```
 TCP   =  통로를 만들고 바이트를 안전하게 나르는 것
 HTTP  =  그 통로로 주고받을 "말의 형식" 을 정한 약속
```

**TCP 는 자기가 뭘 나르는지 모른다.** 그냥 바이트다.
그 바이트에 **의미를 부여하는 규칙**이 HTTP 다.

```
 전화선  =  TCP      목소리를 전달한다. 내용은 모른다
 한국어  =  HTTP     "여보세요 / 네 / 안녕히 계세요" 의 형식을 정한 약속
```

전화선이 있어도 **서로 다른 언어를 쓰면** 대화가 안 된다. 그래서 **약속(프로토콜)** 이 필요하다.

## 계층으로 보면

```
 ┌────────────────────────────────────────┐
 │  내 코드    @GetMapping("/posts")       │
 ├────────────────────────────────────────┤
 │  HTTP      "GET /posts HTTP/1.1"       │  ← 말의 형식
 ├────────────────────────────────────────┤
 │  TCP       연결·순서·재전송              │  ← 통로
 ├────────────────────────────────────────┤
 │  IP        어느 컴퓨터로                 │
 ├────────────────────────────────────────┤
 │  이더넷/WiFi  실제 신호                  │
 └────────────────────────────────────────┘
```

## 💡 우리는 이미 "나만의 프로토콜" 을 만들었다

`TcpEchoServer.java` 를 떠올려 보자.

```java
while ((line = in.readLine()) != null) { ... }
```

**"줄바꿈(`\n`)까지가 한 메시지다"** 라는 규칙을 썼다.
이것도 엄연한 **애플리케이션 프로토콜**이다. 다만 나 혼자만 아는 규칙이다.

```
 내 에코 서버의 규칙        HTTP 의 규칙
 ─────────────────        ─────────────────────────────
 줄바꿈까지가 한 메시지      첫 줄 = 메서드 + 경로 + 버전
 "bye" 면 종료             그다음 = 헤더들
                          빈 줄 = 헤더 끝
                          그 뒤 = 본문
```

**HTTP 는 "모두가 쓰기로 합의한, 표준화된 규칙"** 일 뿐이다.
TCP 위에 얹는 약속이라는 점에서는 내 에코 서버와 똑같다.

---

# 2부. HTTP 메시지는 그냥 텍스트다

## 요청

```
GET /posts/1 HTTP/1.1              ← ① 시작 줄 (메서드 · 경로 · 버전)
Host: localhost:8080               ┐
Accept: application/json           ├ ② 헤더들
User-Agent: curl/8.4.0             ┘
                                   ← ③ 빈 줄 (헤더 끝 신호)
                                   ← ④ 본문 (GET 은 보통 없음)
```

## 응답

```
HTTP/1.1 200 OK                    ← ① 상태 줄 (버전 · 상태코드 · 설명)
Content-Type: application/json     ┐
Content-Length: 85                 ├ ② 헤더들
Date: Fri, 10 Oct 2026 12:00:00    ┘
                                   ← ③ 빈 줄
{"id":1,"title":"첫 글",...}        ← ④ 본문
```

**전부 사람이 읽을 수 있는 글자다.** 특별한 암호도 바이너리도 아니다.

### 빈 줄이 왜 중요한가

TCP 는 바이트를 흘려보낼 뿐이라 **"헤더가 어디서 끝나는지"** 를 모른다.
그래서 **빈 줄 하나**로 "여기까지가 헤더, 다음부터 본문" 을 표시한다.
`Content-Length` 는 "본문이 몇 바이트인지" 를 알려준다.
→ **이런 게 다 "약속" 이다.**

## 직접 손으로 HTTP 요청을 쳐볼 수 있다

```bash
nc 127.0.0.1 8080
```
그리고 아래를 치고 **엔터를 두 번**:
```
GET /posts HTTP/1.1
Host: localhost

```
→ **응답이 온다.** 브라우저도 curl 도 결국 이 글자들을 TCP 로 보내는 것뿐이다.

---

# 3부. HTTPS = HTTP + TLS

## 층이 하나 끼어든다

```
     HTTP                          HTTPS
 ┌──────────┐                  ┌──────────┐
 │   HTTP   │                  │   HTTP   │
 ├──────────┤                  ├──────────┤
 │          │                  │   TLS    │  ← 암호화 층이 추가
 │   TCP    │                  ├──────────┤
 │          │                  │   TCP    │
 └──────────┘                  └──────────┘
   포트 80                       포트 443
```

**HTTP 자체는 하나도 안 바뀐다.** TCP 로 내려가기 전에 **TLS 가 내용을 암호화**할 뿐이다.

## TLS 가 하는 일 세 가지

| | 뭘 보장하나 | 없으면 |
|---|---|---|
| **암호화** | 내용을 못 본다 | 비밀번호가 평문으로 노출 |
| **무결성** | 중간에 **변조되면 들킨다** | 응답이 바꿔치기 당해도 모름 |
| **인증** | **진짜 그 서버**가 맞는지 확인 | 가짜 사이트에 접속해도 모름 |

> **암호화만이 전부가 아니다.** "지금 통신하는 상대가 진짜 네이버인가" 를 확인하는 것도 TLS 의 일이다.
> 그걸 **인증서**가 담당한다.

## 연결 과정 비교

```
 HTTP                             HTTPS
 ① TCP handshake (3-way)          ① TCP handshake (3-way)
 ② 바로 요청                       ② TLS handshake   ← 추가 왕복
                                     · 암호 방식 협상
                                     · 인증서 검증
                                     · 대칭키 교환
                                  ③ 요청 (암호화됨)
```

**HTTPS 가 조금 더 느린 이유**가 이것이다. 그래도 요즘은 거의 다 HTTPS 를 쓴다.
브라우저가 HTTP 사이트에 **"안전하지 않음"** 경고를 띄우기 때문이기도 하다.

---

# 4부. HTTP 말고 다른 통신 방식

| 프로토콜 | 기반 | 특징 | 쓰는 곳 |
|---|---|---|---|
| **HTTP/HTTPS** | TCP | 요청-응답, **상태 없음** | 웹, REST API |
| **WebSocket** | TCP | **양방향**, 연결 유지 | 채팅, 실시간 알림, 주식 시세 |
| **gRPC** | HTTP/2 | **바이너리**, 빠름, 스키마 강제 | 마이크로서비스 간 내부 통신 |
| **GraphQL** | HTTP | 클라이언트가 **필요한 필드만** 요청 | 과다/과소 조회 문제 해결 |
| **MQTT** | TCP | 아주 가벼운 **발행/구독** | IoT 기기 |
| **WebRTC** | UDP | **P2P** 실시간 | 화상통화 |
| DNS | UDP | 도메인 → IP | 주소 조회 |
| SMTP / IMAP | TCP | 메일 송신 / 수신 | 이메일 |
| SSH | TCP | 암호화된 원격 접속 | 서버 관리 |
| FTP | TCP | 파일 전송 | 요즘은 잘 안 씀 |

## HTTP 와 WebSocket 의 차이 — 가장 자주 비교된다

```
 ── HTTP ──  클라이언트가 물어봐야만 답한다
    클라 ── "새 메시지 있어?" ──▶ 서버
    클라 ◀── "없어"            ── 서버
    클라 ── "새 메시지 있어?" ──▶ 서버      ← 계속 물어봐야 한다 (폴링)
    클라 ◀── "없어"            ── 서버

 ── WebSocket ──  서버가 먼저 보낼 수 있다
    클라 ══════ 연결 유지 ══════ 서버
    클라 ◀───── "새 메시지!" ─── 서버      ← 생기면 바로 보낸다
```

**HTTP 는 서버가 먼저 말을 걸 수 없다.** 그래서 채팅·알림에는 WebSocket 을 쓴다.

> 재밌는 점: **WebSocket 도 HTTP 요청으로 시작한다.** `Upgrade: websocket` 헤더를 보내
> "이 연결을 WebSocket 으로 바꾸자" 고 협상한 뒤 전환한다. **왜 굳이?** → 5부 참고.

---

# 5부. 그래서 왜 다들 HTTP 를 쓰나

## ① 방화벽을 통과한다 — 가장 현실적인 이유

```
 회사 / 학교 / 공공 WiFi 의 방화벽
    80 (HTTP)    ✅ 열림
    443 (HTTPS)  ✅ 열림
    9000         ❌ 막힘
    12345        ❌ 막힘
```

**아무리 좋은 프로토콜을 만들어도 방화벽에서 막히면 쓸 수가 없다.**
그래서 새 프로토콜들도 **HTTP 위에 얹거나(gRPC), HTTP 로 시작한다(WebSocket).**

## ② 생태계가 이미 HTTP 를 안다

```
  브라우저 · 프록시 · 캐시 · 로드밸런서 · CDN · API 게이트웨이 · 모니터링
       전부 HTTP 를 이해하도록 만들어져 있다
```

내가 새 프로토콜을 만들면 **저걸 전부 직접 만들어야 한다.**

## ③ 사람이 읽을 수 있다

```
GET /posts HTTP/1.1          ← 그냥 읽힌다
```
**디버깅이 쉽다.** `curl`, 브라우저 개발자도구, `tcpdump` 로 바로 확인된다.
바이너리 프로토콜은 전용 도구 없이는 못 본다.

## ④ 상태가 없어서(stateless) 확장이 쉽다

```
 ── 상태가 있다면 ──            ── HTTP (상태 없음) ──
   요청1 → 서버A (기억함)         요청1 → 서버A  ✅
   요청2 → 서버B ❌ 모름          요청2 → 서버B  ✅
                                 요청3 → 서버C  ✅
   같은 서버로만 가야 한다         아무 서버나 처리 가능 → 서버를 늘리기 쉽다
```

**각 요청이 독립적**이라 서버를 여러 대로 늘리기 쉽다.

> 대신 **로그인 상태를 기억할 수 없다.** 그래서 **쿠키 / 세션 / JWT** 가 필요하다.
> → 이게 다음 CS 주제다.

## ⑤ 표준이다

언어·플랫폼·회사가 달라도 통한다. Java 서버와 iOS 앱과 파이썬 스크립트가 같은 API 를 쓴다.

---

# 6부. 용어 정리

| 용어 | 한 줄 |
|---|---|
| **프로토콜** | 통신할 때 지키기로 한 **약속·형식** |
| **TCP** | 바이트를 **안전하게 나르는 통로** |
| **HTTP** | 그 통로로 주고받을 **메시지 형식**을 정한 약속 |
| **HTTPS** | HTTP + **TLS 암호화 층** |
| **TLS** | 암호화 · 무결성 · **서버 인증**을 담당 (예전 이름 SSL) |
| **포트 80 / 443** | HTTP / HTTPS 의 기본 포트 |
| **stateless** | 요청끼리 서로 **모른다**. 그래서 쿠키·세션이 필요 |
| **인증서** | "이 서버가 진짜 맞다" 를 증명하는 문서 |
| **WebSocket** | 연결을 유지하며 **서버가 먼저 보낼 수 있는** 프로토콜 |

---

# 면접 포인트

1. **TCP 와 HTTP 의 관계는?**
   → TCP 는 **바이트를 안전하게 나르는 전송 계층**이고, HTTP 는 그 위에서
   **주고받을 메시지 형식을 정한 응용 계층 프로토콜**이다. HTTP 는 TCP 없이 동작하지 않는다.

2. **HTTP 와 HTTPS 의 차이는?**
   → HTTPS 는 HTTP 와 TCP 사이에 **TLS 층**이 들어간 것이다. **암호화·무결성·서버 인증**을
   제공한다. 포트는 80 → 443.

3. **HTTP 가 stateless 하다는 건 무슨 뜻인가요?**
   → 각 요청이 **독립적**이라 이전 요청을 기억하지 않는다. 서버 확장이 쉽지만,
   로그인 상태 유지를 위해 **쿠키·세션·JWT** 가 필요하다.

4. **왜 다들 HTTP 를 쓰나요?**
   → ① **방화벽이 80/443 만 열어둔다** ② 프록시·캐시·로드밸런서 등 **생태계가 전부 HTTP 기반**
   ③ 텍스트라 **디버깅이 쉽다** ④ stateless 라 **수평 확장이 쉽다**.
   그래서 gRPC 도 HTTP/2 위에서, WebSocket 도 HTTP 로 시작해 전환한다.

5. **HTTP 와 WebSocket 은 언제 나눠 쓰나요?**
   → HTTP 는 **클라이언트가 물어야만 답한다.** 서버가 먼저 알려줘야 하는
   채팅·알림·실시간 시세는 **WebSocket** 을 쓴다.

---

# 7부. 실습 3 — HTTP 는 평문이다

## 준비

```bash
cd ~/Desktop/study/cs/network/lab
```

터미널 **3개**를 쓴다.

## Step 1. HTTP 서버 실행

**터미널 1**
```bash
java HttpLoginServer.java
```

## Step 2. 패킷 캡처 — `-A` 옵션이 핵심

**터미널 2**
```bash
sudo tcpdump -i lo0 -n -A 'tcp port 8080'
```

| 옵션 | 뜻 |
|---|---|
| `-A` | **패킷 내용을 ASCII(글자)로 출력** ← 이번 실습의 핵심 |
| `-n` | 주소를 숫자로 |

## Step 3. 로그인 요청 — ⚠️ 여기서 보게 된다

**터미널 3**
```bash
curl -X POST http://127.0.0.1:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"donghyun","password":"MySecret1234"}'
```

**터미널 2를 보라.**

```
POST /login HTTP/1.1
Host: 127.0.0.1:8080
User-Agent: curl/8.4.0
Content-Type: application/json
Content-Length: 48

{"username":"donghyun","password":"MySecret1234"}
                                    ▲
                        비밀번호가 그대로 보인다 😱
```

응답도 마찬가지다.
```
HTTP/1.1 200 OK
Content-Type: application/json

{"result":"ok","token":"eyJhbGciOiJIUzI1NiJ9.fake-token","user":"donghyun"}
                        ▲
              인증 토큰까지 그대로 노출
```

### 이게 왜 위험한가

```
 카페 공용 WiFi
   나 ──────▶ 공유기 ──────▶ 인터넷
              ▲
       같은 WiFi 에 붙은 누군가가
       tcpdump 를 켜두면 똑같이 보인다
```

**네트워크 경로 위의 누구나** 볼 수 있다. 공유기, ISP, 중간 라우터 전부.

> 2부에서 **"HTTP 메시지는 그냥 텍스트"** 라고 했다. 이게 그 증거다.
> 암호화가 없으면 **글자가 선을 타고 그대로 흐른다.**

## 보너스 — 손으로 HTTP 요청 쳐보기

```bash
nc 127.0.0.1 8080
```
아래를 치고 **엔터 두 번**:
```
POST /login HTTP/1.1
Host: 127.0.0.1
Content-Type: application/json
Content-Length: 48

{"username":"donghyun","password":"MySecret1234"}
```
→ **응답이 온다.** curl 도 브라우저도 결국 이 글자를 TCP 로 보낼 뿐이다.

---

# 8부. 실습 4 — HTTPS 로 바꿔보기

## Step 1. 인증서 만들기 (이미 만들어져 있다면 건너뛴다)

```bash
keytool -genkeypair -alias lab -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -storepass labpass \
  -validity 365 -dname "CN=localhost, OU=lab, O=study, L=Seoul, C=KR" \
  -ext "SAN=dns:localhost,ip:127.0.0.1"
```

| 조각 | 뜻 |
|---|---|
| `-genkeypair` | **개인키 + 공개키 쌍**을 만든다 |
| `-keystore keystore.p12` | 그걸 담아둘 금고 파일 |
| `-dname "CN=localhost..."` | **이 인증서의 주인이 누구인지** |
| `-ext "SAN=..."` | 이 인증서가 **어느 도메인/IP 에 유효한지** |

**자체 서명(self-signed) 인증서**다. 내가 나를 보증하는 셈이라 브라우저는 신뢰하지 않는다.
(→ 9부에서 왜 그런지 본다)

## Step 2. HTTPS 서버 실행

**터미널 1** (HTTP 서버는 꺼도 된다)
```bash
java HttpsLoginServer.java
```

> `HttpLoginServer.java` 와 **코드를 비교해보라.** 로그인 처리 부분은 **똑같다.**
> TLS 설정 10줄만 추가됐다. → **HTTP 는 하나도 안 바뀌고 아래에 층만 끼었다는 증거.**

## Step 3. 같은 방식으로 캡처

**터미널 2**
```bash
sudo tcpdump -i lo0 -n -A 'tcp port 8443'
```

**터미널 3**
```bash
curl -k -X POST https://127.0.0.1:8443/login \
  -H "Content-Type: application/json" \
  -d '{"username":"donghyun","password":"MySecret1234"}'
```
> `-k` 는 "자체 서명 인증서라도 그냥 진행" 이라는 뜻이다. 없으면 curl 이 거부한다.

**터미널 2를 보라.**

```
IP 127.0.0.1.52841 > 127.0.0.1.8443: Flags [P.], length 517
.....z.=.?.$..j...q.....9..8..5....
...6..*...?..v.(..}.....|..{.....k..j
                ▲
        알아볼 수 없는 바이트 뿐이다 ✅
```

**`password` 라는 글자가 어디에도 없다.**

## Step 4. ⭐ 그런데 이건 여전히 보인다

```
IP 127.0.0.1.52841 > 127.0.0.1.8443
   └─ 출발 IP/포트 ─┘  └─ 도착 IP/포트 ─┘    ← 평문!
                                 length 517  ← 데이터 크기도 보인다
```

| HTTPS 가 가려주는 것 | HTTPS 로도 노출되는 것 |
|---|---|
| 요청 본문 (비밀번호) | **누가(출발 IP)** |
| 응답 본문 (토큰) | **어디에(도착 IP·포트)** |
| URL 경로 (`/login`) | **언제** |
| HTTP 헤더 | **얼마나** (데이터 크기) |

**당연하다.** 라우터가 **어디로 보낼지 알아야** 배달이 되니까.
주소까지 암호화하면 전달 자체가 불가능하다.

> **"HTTPS 면 완전히 익명" 이라는 건 오해다.**
> **접속했다는 사실 자체는 숨겨지지 않는다.** 내용만 못 볼 뿐이다.

---

# 9부. TLS handshake 들여다보기

```bash
openssl s_client -connect 127.0.0.1:8443 -servername localhost < /dev/null
```

볼 것:
```
Certificate chain
 0 s:C=KR, L=Seoul, O=study, OU=lab, CN=localhost      ← 주체(subject)
   i:C=KR, L=Seoul, O=study, OU=lab, CN=localhost      ← 발급자(issuer)
   ▲ 주체와 발급자가 같다 = 자체 서명

Verify return code: 18 (self signed certificate)       ← 그래서 검증 실패

New, TLSv1.3, Cipher is TLS_AES_256_GCM_SHA384         ← 협상된 암호 방식
```

## 실제 사이트와 비교해보라

```bash
openssl s_client -connect www.naver.com:443 -servername www.naver.com < /dev/null 2>/dev/null | head -20
```

```
Certificate chain
 0 s:CN=www.naver.com          ← 네이버
   i:CN=DigiCert ...           ← 발급자가 "다른 기관"
 1 s:CN=DigiCert ...           ← 그 기관의 인증서
   i:CN=DigiCert Global Root   ← 또 그 위의 기관

Verify return code: 0 (ok)     ← 검증 성공
```

## 왜 브라우저가 경고를 띄우나

```
 ── 실제 사이트 ──                     ── 우리 실습 ──
   네이버 인증서                          내 인증서
      ▲ 서명                               ▲ 서명
   중간 CA                                 내가
      ▲ 서명                                 ▲
   루트 CA  ← OS/브라우저에 미리 내장       (아무도 보증 안 함)

   신뢰의 사슬이 루트까지 이어진다 ✅      사슬이 없다 ❌ → 경고
```

**"신뢰할 수 있는 기관이 보증했는가"** 를 확인하는 것이 인증서 검증이다.
자체 서명은 **"내가 나를 보증" 하는 것**이라 아무 의미가 없다.

> 그래서 TLS 는 암호화만이 아니라 **"상대가 진짜인지 확인" 하는 역할**도 한다.
> 암호화만 있고 인증이 없으면 **중간자가 가짜 서버 행세**를 할 수 있다.

---

# 10부. 체크리스트

### HTTP / HTTPS
- [ ] `tcpdump -A` 에서 비밀번호가 보인 이유는?
- [ ] HTTPS 로 바꾸니 안 보이는 이유는? 어느 층이 한 일인가?
- [ ] HTTPS 인데도 **IP 와 포트가 보이는** 이유는?
- [ ] `HttpLoginServer` 와 `HttpsLoginServer` 의 **코드 차이**는 어디뿐이었나? 그게 무엇을 뜻하나?
- [ ] 자체 서명 인증서를 브라우저가 거부하는 이유는?
- [ ] TLS 가 보장하는 세 가지는?

### TCP 복습
- [ ] HTTPS 요청 하나가 나가기까지 거치는 단계를 순서대로 말해보라
- [ ] HTTP 는 왜 TCP 를 쓰고 DNS 는 왜 UDP 를 쓰나?
- [ ] `Content-Length` 헤더가 필요한 이유는? (TCP 의 어떤 성질 때문인가)

---

# 11부. 실무 구성 — TLS termination 과 데이터 흐름

## 전체 배치

```
  ┌─────────┐         ┌──────────────────┐         ┌──────────────┐      ┌─────┐
  │ 브라우저 │         │  로드밸런서/Nginx  │         │ Spring Boot  │      │ DB  │
  │         │ ◀─HTTPS─▶│                  │ ◀─HTTP─▶ │   :8080      │ ◀──▶ │     │
  └─────────┘   :443   │  🔑 인증서 보관   │  :8080  │  (평문)       │      └─────┘
                       └──────────────────┘         └──────────────┘
   ─────── 인터넷 ──────┤                └───────── 내부망 ──────────┘
                        ▲
                 TLS termination  (여기서 암호를 푼다)
```

## 왜 앱이 직접 HTTPS 를 안 하나

| 이유 | |
|---|---|
| **인증서 관리 집중** | 서버가 50대여도 인증서는 LB 한 곳에만. 갱신도 한 번 |
| **CPU 부담 분리** | TLS 암복호화는 CPU 를 쓴다. 전용 장비/프록시가 처리 |
| **앱은 로직에만 집중** | Spring Boot 가 인증서를 몰라도 된다 |
| **갱신 자동화** | Let's Encrypt 자동 갱신을 LB 에서 한 번만 설정 |

---

## 요청이 가는 길 — 데이터가 어떻게 바뀌나

### ① 브라우저 — 객체에서 시작
```javascript
{ username: "donghyun", password: "MySecret1234" }
```
```
      ▼ JSON 직렬화
{"username":"donghyun","password":"MySecret1234"}        48 bytes
```

### ② HTTP 메시지로 포장
```
POST /login HTTP/1.1
Host: api.myapp.com
Content-Type: application/json
Content-Length: 48
                                        ← 빈 줄
{"username":"donghyun","password":"MySecret1234"}
```

### ③ TLS 암호화 ⭐
```
      ▼ 합의된 대칭키로 암호화 + 무결성 태그
17 03 03 01 f5 8f 3a 9d 2b 7c 1e a4 ...
      ▲ 헤더도 본문도 전부 암호화된다
```

### ④ TCP/IP 헤더 붙이기
```
┌──────────┬───────────┬────────────────────────────────────┐
│ IP 헤더   │ TCP 헤더   │ 17 03 03 01 f5 8f 3a 9d ...        │
│ 출발/도착 │ 포트/seq   │        (암호문)                     │
│  평문 ⚠️  │  평문 ⚠️   │                                    │
└──────────┴───────────┴────────────────────────────────────┘
```

### ⑤ 인터넷 구간 — tcpdump 를 떠도 암호문
```
  📡 WiFi → 공유기 → ISP → 라우터 → 라우터 → LB
  └──────────── 전부 암호문만 본다 ✅ ────────────┘
```

### ⑥ 로드밸런서 — TLS 를 푼다
```
      ▼ TCP/IP 헤더 벗김
      ▼ TLS 복호화  ← 🔑 인증서와 개인키가 여기 있다

POST /login HTTP/1.1
Host: api.myapp.com
{"username":"donghyun","password":"MySecret1234"}      ← 평문 복원! ⚠️

      ▼ 헤더를 추가한다
X-Forwarded-For: 203.0.113.42          ← 원래 클라이언트의 IP
X-Forwarded-Proto: https               ← 원래는 HTTPS 였다는 표시
X-Forwarded-Port: 443
```

### ⑦ 내부망 — 평문 HTTP 로 전달
```
  LB ──── HTTP (평문) ────▶ Spring Boot :8080
  ⚠️ 여기서 tcpdump 를 뜨면 비밀번호가 보인다
     "내부망이니 괜찮다" 고 보는 것뿐이다
```

### ⑧ Spring Boot — 객체로 복원
```
      ▼ DispatcherServlet → HandlerMapping → LoginController.login()
      ▼ HttpMessageConverter (Jackson)
LoginRequest { username="donghyun", password="MySecret1234" }
      ▼ @Valid 검증 → Service → Repository → DB (해시 비교)
LoginResponse { token="eyJhbGciOiJIUzI1NiJ9..." }
```

---

## 응답이 오는 길 — 역순

```
 Spring Boot
   LoginResponse 객체
        ▼ Jackson 직렬화
   {"token":"eyJhbGciOiJIUzI1NiJ9..."}
        ▼ HTTP 응답 조립
   HTTP/1.1 200 OK ...
        │  HTTP 평문으로 LB 까지
        ▼
 로드밸런서
        ▼ TLS 암호화 (다시 잠근다) 🔒
   17 03 03 00 6a 4f 2c ...
        │  인터넷 구간은 암호문
        ▼
 브라우저
        ▼ TLS 복호화 → JSON 파싱
   { token: "eyJ..." }
```

**LB 가 "암호를 풀고 → 다시 잠그는" 중계소 역할이다.**

---

## ⭐ `X-Forwarded-*` 헤더 — 실무에서 꼭 걸리는 지점

LB 를 거치면 Spring Boot 입장에서는 **접속자가 전부 LB** 로 보인다.

```
 request.getRemoteAddr()
      ├─ LB 없을 때   →  203.0.113.42   (진짜 클라이언트)
      └─ LB 있을 때   →  10.0.1.5       (LB 의 내부 IP) 💥
```

**접속 로그, IP 차단, 지역 판별이 전부 망가진다.** 그래서 LB 가 원래 정보를 헤더에 넣어준다.

| 헤더 | 담는 것 |
|---|---|
| `X-Forwarded-For` | 원래 클라이언트 IP |
| `X-Forwarded-Proto` | 원래 스킴 (`https`) |
| `X-Forwarded-Port` | 원래 포트 (443) |

```yaml
server:
  forward-headers-strategy: framework     # 이 한 줄이면 Spring 이 읽어준다
```

**`X-Forwarded-Proto` 가 특히 중요한 이유**
```
 Spring Boot 입장:  "나한테 HTTP 로 들어왔네"
 리다이렉트 생성:    http://api.myapp.com/...     💥 HTTPS 였는데 HTTP 로!
```

> ⚠️ 이 헤더는 **클라이언트가 위조할 수 있다.** 신뢰할 수 있는 LB 를 거친 경우에만 믿도록 설정해야 한다.

---

## 구간별 보안 수준

```
 ① 브라우저 메모리      평문   (당연)
 ② 브라우저 ~ LB       암호문  ✅ ← TLS 가 지키는 구간
 ③ LB 내부 (메모리)     평문   ⚠️ 복호화 지점
 ④ LB ~ Spring Boot    평문   ⚠️ 내부망이라 '괜찮다'고 보는 것
 ⑤ Spring Boot 메모리   평문   ⚠️
 ⑥ Spring ~ DB         평문 또는 TLS
```

| 규칙 | 이유 |
|---|---|
| **로그에 비밀번호를 찍지 마라** | ⑤에서 평문이다. 로그로 나가면 영구 보존된다 |
| **비밀번호는 해시로 저장** | TLS 는 ②만 지킨다. DB 까지 보호해주지 않는다 |
| **내부망도 암호화 (mTLS)** | ④를 못 믿는 환경(Zero Trust)에서 |

> **전송 보안(TLS)과 저장·기록 보안은 완전히 별개 문제다.**

---

## 한 장 요약

```
  브라우저          인터넷           LB            내부망        Spring Boot
 ─────────────────────────────────────────────────────────────────────────
  객체
   ↓ 직렬화
  JSON 평문
   ↓ HTTP 포장
  HTTP 메시지
   ↓ TLS 암호화
  암호문        ══════════▶   암호문
                              ↓ TLS 복호화 🔑
                             HTTP 평문
                              ↓ X-Forwarded-* 추가
                             HTTP 평문   ═══════════▶   HTTP 평문
                                                         ↓ Jackson
                                                        객체
                                                         ↓ 비즈니스 로직
```

**양 끝은 객체, 중간은 텍스트, 인터넷 구간만 암호문.**

---

## 프론트엔드는 TLS 를 신경 쓰나 — 아니다

```javascript
fetch('https://api.myapp.com/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});
```

**이게 전부다.** 암호화 코드가 한 줄도 없다.
`https://` 로 시작하는 URL 이면 **브라우저가 알아서** TCP 연결 → TLS handshake → 암호화를 한다.

```
  JS 코드         fetch()        브라우저 네트워크 스택        네트워크
 ─────────────────────────────────────────────────────────────────────
  객체  ──▶  HTTP 메시지  ──▶   TLS 암호화 + TCP/IP   ──▶   선을 탄다
             ▲                    ▲
      [개발자도구가 보여주는 곳]   [tcpdump 가 보는 곳]
```

### ⚠️ 개발자도구에 평문이 보이는 이유

**개발자도구는 암호화 "전" 을 보여준다.** 브라우저 안에서 가로채 보여주는 것이라 당연히 평문이다.
`tcpdump` 는 **선 위** 를 보므로 암호문이다. **둘은 보는 지점이 다르다.**

### 흔한 오해 — "프론트에서 비밀번호를 해시해서 보내면 더 안전하다"

**아니다. 오히려 해롭다.**
```
 해시해서 보내면 → 그 "해시값" 자체가 비밀번호가 된다
                   탈취당하면 그대로 로그인 가능 💥
```
**TLS 가 이미 전송 구간을 보호한다.** 해시는 **서버가 저장할 때** 하는 일이다.

---

# 12부. HTTPS 요청 하나의 전체 흐름 (단계별 · 입장별)

## 먼저 — 흔한 오해부터

```
 ❌ "클라이언트가 알아서 암호화해서 보낸다. 서버는 그걸 푸는 코드가 필요하다"

 ✅ 암호화하려면 "키" 가 필요하고, 그 키는 서버와 "합의" 해야 만들어진다
    → 서버가 참여하지 못하면 암호화 자체가 시작되지 않는다
```

### 증명 — HTTP 서버에 `https://` 로 요청하면

```bash
java HttpLoginServer.java              # 평범한 HTTP 서버 (8080)
curl https://127.0.0.1:8080/login      # https 로 요청
→ 응답 없이 멈춘다 💥 (타임아웃)
```

**curl 이 TLS ClientHello 를 보냈는데, HTTP 서버는 그게 뭔지 몰라 아무 응답도 못 한다.**
curl 은 TLS 응답을 영원히 기다린다.

### 그래서 `HttpsLoginServer` 의 10줄은

| 오해 | 실제 |
|---|---|
| "복호화하려고" | **handshake 에 참여하려고** |

```java
KeyStore keyStore = ...load(keystore.p12)    // 🔑 개인키 + 📜 인증서
SSLContext sslContext = ...init(keyManagers) // TLS 엔진
server.setHttpsConfigurator(...)             // 서버에 장착
```

| handshake 에서 서버가 하는 일 | 필요한 재료 |
|---|---|
| **인증서를 보낸다** ("나 이 도메인 주인이야") | 📜 인증서 |
| **개인키로 서명한다** ("진짜 주인임을 증명") | 🔑 개인키 |

**재료가 없으면 → handshake 실패 → 대칭키 합의 불가 → 암호화 자체가 불가능.**

---

## 단계 0 — 시작

> **[브라우저 입장]**
```
https://api.myapp.com/login
  ▲ 스킴이 https → "443 포트로, TLS 를 써서 접속해라"
```

## 단계 ① DNS 조회 (UDP)

> **[브라우저 입장]** "이 도메인의 IP 를 모르겠다"
```
 브라우저 ──UDP──▶ DNS 서버    "api.myapp.com 의 IP 가 뭐야?"  (약 30 bytes)
 브라우저 ◀──UDP── DNS 서버    "203.0.113.10"                (약 50 bytes)
```
> **[선 위]** `UDP, length 30` — handshake 없이 바로 데이터
> **왜 UDP?** 질의가 짧아 **연결 비용이 데이터보다 크다**

## 단계 ② TCP 3-way handshake

> **[브라우저 입장]** "203.0.113.10 의 443 포트로 통로를 만들자"
```
 브라우저 ── [S]  SYN      ──▶ 서버    "연결하자. 내 seq 는 x"
 브라우저 ◀─ [S.] SYN+ACK  ── 서버    "좋아. 내 seq 는 y"
 브라우저 ── [.]  ACK      ──▶ 서버    "확인"
```
> **[선 위]** `Flags [S]` `[S.]` `[.]` — TCP 실습에서 본 그것
> **[이 시점]** 바이트 통로 완성. **아직 암호화는 하나도 없다**

## 단계 ③ TLS handshake ⭐

### ③-1 ClientHello
```
 브라우저 ──────────────────────────────────▶ 서버

 "TLS 1.3 쓸 수 있어"
 "이런 암호방식 지원해: AES-256-GCM, CHACHA20..."
 "내 키 재료(물감 🟢) 여기 있어"
 "접속하려는 도메인은 api.myapp.com"   ← SNI
```
> **[선 위]** 아직 **평문**. 암호 방식을 정하는 중이라 암호화할 수 없다

### ③-2 ServerHello + 인증서
```
 브라우저 ◀────────────────────────────────── 서버

 "AES-256-GCM 으로 하자"
 "내 키 재료(물감 🟠) 여기 있어"
 "이게 내 인증서야"           ← 📜 keystore 에서 꺼낸다
 "개인키로 서명했어, 확인해봐"  ← 🔑 keystore 로 서명한다
```
> **[서버 입장]** **여기서 그 10줄이 일한다.** keystore 가 없으면 이 단계를 못 한다

### ③-3 브라우저가 검증
> **[브라우저 입장]** 네 가지를 확인한다
```
 ① 인증서 사슬이 "내장된 루트 CA" 까지 이어지나?
 ② 인증서의 도메인이 api.myapp.com 과 일치하나?
 ③ 유효기간이 지나지 않았나?
 ④ 서명이 인증서 속 공개키로 풀리나? (= 개인키를 진짜 갖고 있나)

 하나라도 실패 → 경고 또는 연결 중단
```
> 우리 실습은 **①에서 실패** → `curl -k` 로 건너뛴 것

### ③-4 대칭키 계산
```
 브라우저                                서버
  🟢 + 🟠 → 🟤                          🟠 + 🟢 → 🟤
       └────── 같은 대칭키 ──────┘
```
> **[선 위]** 🟢 과 🟠 만 보인다. **🟤 는 어디에도 흐르지 않았다**

## 단계 ④ HTTP 요청

> **[브라우저 입장]** 데이터가 이렇게 변한다
```
 { username: "donghyun", password: "MySecret1234" }     객체
        ▼ JSON 직렬화
 {"username":"donghyun","password":"MySecret1234"}      평문 48 bytes
        ▼ HTTP 메시지로 포장
 POST /login HTTP/1.1
 Host: api.myapp.com
 Content-Length: 48

 {"username":"donghyun","password":"MySecret1234"}
        ▼ 🟤 대칭키로 암호화 + 무결성 태그
 17 03 03 01 f5 8f 3a 9d ...
        ▼ TCP/IP 헤더
 [IP헤더][TCP헤더][암호문]
```
> **[선 위]** `Flags [P.]` + 알아볼 수 없는 바이트. **IP·포트만 평문**

> **[서버 입장]** 역순으로 푼다
```
 [IP헤더][TCP헤더][암호문]
        ▼ TCP/IP 벗김
        ▼ 🟤 대칭키로 복호화 + 태그 검증
 POST /login HTTP/1.1 ... {"username":"donghyun",...}
        ▼ HTTP 파싱 (HttpServer 가)
        ▼ (Spring 이면 Jackson)
 LoginRequest 객체
```

## 단계 ⑤ HTTP 응답 — 역순
```
 [서버]    객체 → JSON → HTTP 응답 → 🟤 암호화 → 전송
 [선 위]                            암호문
 [브라우저] 🟤 복호화 → HTTP 파싱 → JSON 파싱 → 객체
```

## 단계 ⑥ 종료
```
 [F.] FIN / [.] ACK / [F.] FIN / [.] ACK
```
> keep-alive 면 바로 안 끊고 재사용한다. 매 요청마다 ②③ 을 반복하면 너무 비싸다

---

## 한눈에 — 각 단계가 선 위에서 어떻게 보이나

| 단계 | 누가 주도 | 선 위에서 | 암호화 |
|---|---|---|---|
| ① DNS | 브라우저 | `UDP, length 30` | ❌ |
| ② TCP handshake | 양쪽 | `[S] [S.] [.]` | ❌ |
| ③ TLS handshake | 양쪽 | `[P.]` 평문 → 암호문 | 중간부터 ✅ |
| ④ HTTP 요청 | 브라우저 | `[P.]` 암호문 | ✅ |
| ⑤ HTTP 응답 | 서버 | `[P.]` 암호문 | ✅ |
| ⑥ 종료 | 한쪽 | `[F.] [.] [F.] [.]` | — |

**②와 ③을 합쳐 왕복이 2~3번.**
HTTP/3(QUIC)가 UDP 로 간 이유가 **이걸 1번으로 줄이려고** 다.

---

## 한 문장

```
 TLS 는 "한쪽이 거는 자물쇠" 가 아니라
 "양쪽이 같은 열쇠를 만드는 과정" 이다
```
