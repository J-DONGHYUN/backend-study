# JDK / JRE / JVM, Gradle, Gradle Wrapper

> 참고용 정리. 읽고 나서 **내 말로 요약한 노트는 따로** 작성할 것.

---

## 1. JDK / JRE / JVM — 포함 관계

```
┌─────────────────────────────────────────────────┐
│  JDK (Java Development Kit)                     │
│  "개발자용 전체 도구 세트"                        │
│                                                 │
│   javac(컴파일러)   jar   javadoc   jdb(디버거)   │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │  JRE (Java Runtime Environment)           │  │
│  │  "실행에 필요한 것만"                       │  │
│  │                                           │  │
│  │   표준 라이브러리 (java.util, java.io ...)  │  │
│  │                                           │  │
│  │  ┌─────────────────────────────────────┐  │  │
│  │  │  JVM (Java Virtual Machine)         │  │  │
│  │  │  "바이트코드를 실제로 실행하는 엔진"    │  │  │
│  │  │   + GC(메모리 청소)                   │  │  │
│  │  └─────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

| | 역할 | 누가 필요한가 |
|---|---|---|
| **JVM** | 바이트코드 실행, 메모리 관리(GC) | 모두 |
| **JRE** | JVM + 표준 라이브러리 | 실행만 하는 사람 |
| **JDK** | JRE + 컴파일러 등 개발 도구 | **개발자** |

**한 줄 요약:** JDK ⊃ JRE ⊃ JVM

> Java 11부터 **JRE 단독 배포가 중단**됐습니다. 지금은 그냥 JDK를 설치합니다.
> "JDK와 JRE 차이"는 여전히 면접 단골 질문입니다.

---

## 2. 소스 코드가 실행되기까지

```
  Todo.java              Todo.class                실행
  (사람이 읽는 소스)  ──▶  (바이트코드)        ──▶   (프로그램 동작)
                   javac                  java
                   = JDK가 제공            = JVM이 해석

                   [컴파일 타임]           [런타임]
```

**왜 중간에 바이트코드 단계가 있나?**

C 같은 언어는 소스를 **OS별 기계어**로 바로 컴파일합니다. 그래서 Windows용, Mac용을 따로 만들어야 합니다.

Java는 OS와 무관한 **바이트코드** 하나만 만들고, 각 OS용 JVM이 그걸 번역합니다.

```
                    ┌──▶  Windows JVM  ──▶  Windows에서 실행
   Todo.class  ─────┼──▶  Mac JVM      ──▶  Mac에서 실행
   (하나만 만듦)      └──▶  Linux JVM    ──▶  Linux에서 실행
```

이게 **"Write Once, Run Anywhere"** 입니다.

---

## 3. Gradle — 왜 필요한가

### 빌드 도구가 없다면

프로젝트가 커지면 이런 일을 **손으로** 해야 합니다.

| 문제 | 수작업하면 |
|---|---|
| 소스 파일 수백 개 컴파일 | `javac` 뒤에 파일 경로를 전부 나열 |
| Spring, H2 같은 라이브러리 사용 | 사이트에서 jar 직접 다운로드 → classpath에 등록 |
| **전이 의존성** | Spring이 또 다른 라이브러리 30개를 요구 → 그것도 전부 수동 |
| 버전 충돌 | A는 라이브러리 v1, B는 v2 필요 → 직접 조율 |
| 테스트 실행 / jar 생성 | 매번 수동 명령 |

**Gradle이 이걸 전부 자동화합니다.**

### Gradle이 하는 일

```
   build.gradle  (내가 "뭘 쓸지" 선언)
        │
        ▼
   ┌─────────────────────────────────────────────┐
   │  Gradle                                     │
   │                                             │
   │  1. 의존성 해결  ──▶  Maven Central에서 다운로드│
   │  2. 컴파일       ──▶  javac 호출             │
   │  3. 테스트 실행   ──▶  JUnit 실행            │
   │  4. 패키징       ──▶  실행 가능한 jar 생성    │
   └─────────────────────────────────────────────┘
```

### build.gradle 읽기

```gradle
plugins {
    id 'java'                                        // Java 프로젝트다
    id 'org.springframework.boot' version '4.1.1'    // Spring Boot 기능 추가
    id 'io.spring.dependency-management' ...         // 라이브러리 버전 자동 관리
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17) // Java 17 문법/API로 컴파일
    }
}

repositories {
    mavenCentral()        // 라이브러리를 어디서 받아올지
}

dependencies {
    implementation '...data-jpa'    // 앱 실행에 필요
    runtimeOnly 'com.h2database:h2' // 실행 때만 필요 (컴파일엔 불필요)
    testImplementation '...'        // 테스트할 때만 필요
}
```

**의존성 scope 구분이 중요합니다.**

| scope | 언제 쓰이나 | 예 |
|---|---|---|
| `implementation` | 컴파일 + 실행 | Spring Web, JPA |
| `runtimeOnly` | 실행할 때만 | **H2 드라이버** — 내 코드가 직접 부르지 않고 JDBC가 찾아 씀 |
| `testImplementation` | 테스트할 때만 | JUnit |

> `toolchain`이 17인데 IntelliJ에 설치된 JDK는 25일 수 있습니다.
> **설치된 JDK 버전과, 그걸로 "몇 버전 문법으로 컴파일할지"는 별개**입니다.

### Gradle vs Maven

| | Gradle | Maven |
|---|---|---|
| 설정 파일 | `build.gradle` (Groovy/Kotlin **코드**) | `pom.xml` (**XML**) |
| 문법 | 짧음 | 장황함 |
| 속도 | 빠름 (증분 빌드, 캐시) | 상대적으로 느림 |

요즘 신규 프로젝트는 대부분 Gradle을 씁니다.

---

## 4. Gradle Wrapper (`gradlew`)

### 해결하는 문제

```
   팀원 A: Gradle 8.5 설치       ─┐
   팀원 B: Gradle 9.7 설치       ─┼──▶  빌드 결과가 서로 다름 💥
   CI 서버: Gradle 7.0 설치      ─┘      "제 컴퓨터에선 되는데요"
```

### 해결 방식

프로젝트 안에 **"이 프로젝트는 Gradle 9.7.1을 쓴다"**를 기록하고, 없으면 **자동으로 받아서** 씁니다.

```
프로젝트/
├── gradlew                    ← 실행 스크립트 (Mac/Linux)
├── gradlew.bat                ← 실행 스크립트 (Windows)
└── gradle/wrapper/
    ├── gradle-wrapper.jar
    └── gradle-wrapper.properties   ← 여기에 버전이 적혀 있음
```

### `./gradlew test` 실행 시 일어나는 일

```
  ./gradlew test
       │
       ├─ 1. gradle-wrapper.properties에서 버전 확인 (예: 9.7.1)
       │
       ├─ 2. ~/.gradle/ 에 그 버전이 있나?
       │       없으면 ──▶ 자동 다운로드
       │
       └─ 3. 그 버전의 Gradle로 'test' task 실행
```

**→ 팀원 누구든, CI 서버든 똑같은 빌드 결과.**
그래서 `gradlew`와 `gradle/` 폴더는 **반드시 Git에 커밋**합니다.

> `./`는 "현재 디렉토리의 이 파일을 실행하라"는 뜻입니다.
> 그냥 `gradlew`라고 치면 셸이 PATH에서 찾기 때문에 못 찾습니다.

---

## 5. 전체 그림

```
   build.gradle                          [내가 선언]
        │
        ▼
   ./gradlew  ──▶  Gradle
                     │
                     ├──▶  의존성 다운로드  ◀── Maven Central
                     │
                     ├──▶  javac 컴파일    ──▶  build/classes/*.class
                     │
                     ├──▶  테스트 실행      ──▶  build/reports/tests/
                     │
                     └──▶  jar 패키징      ──▶  build/libs/*.jar
                                                      │
                                                      ▼
                                              java -jar ──▶ JVM 실행
```

### 자주 쓰는 명령어

```bash
./gradlew test       # 테스트만 실행
./gradlew bootRun    # 앱 실행 (IntelliJ ▶ 버튼과 동일)
./gradlew build      # 컴파일 + 테스트 + jar 생성
./gradlew clean      # build 폴더 삭제 (빌드가 꼬였을 때)
```

### 폴더별 의미

| 폴더 | 뭔가 | Git 커밋? |
|---|---|---|
| `src/` | **내가 쓴 소스** | ✅ |
| `build/` | Gradle이 만든 결과물 (.class, jar, 리포트) | ❌ 언제든 재생성 가능 |
| `.gradle/` | Gradle 캐시 | ❌ |
| `gradle/`, `gradlew` | **Wrapper** | ✅ 반드시 |

---

## 면접 포인트

1. **JDK와 JRE의 차이는?** → JDK = JRE + 개발 도구(javac 등). JRE = JVM + 표준 라이브러리.
2. **Java가 플랫폼 독립적인 이유는?** → 바이트코드로 컴파일하고, OS별 JVM이 번역해서 실행.
3. **Gradle Wrapper를 왜 쓰나?** → 모든 환경에서 동일한 Gradle 버전을 보장해 빌드 재현성을 확보.
4. **`implementation`과 `runtimeOnly`의 차이는?** → 컴파일 시점에 필요한지 여부. H2 드라이버는 내 코드가 직접 참조하지 않으므로 `runtimeOnly`.
