import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.LocalTime;

/**
 * [실습 4] HTTPS 로그인 서버 — 같은 API 인데 암호화된다
 *
 * HttpLoginServer 와 "코드가 거의 같다". 달라진 건 TLS 설정뿐이다.
 * → HTTP 자체는 하나도 안 바뀌고 아래에 TLS 층만 끼었다는 뜻.
 *
 * 준비:  keystore.p12 가 필요하다 (아래 keytool 명령으로 생성)
 * 실행:  java HttpsLoginServer.java
 * 호출:  curl -k -X POST https://127.0.0.1:8443/login ...
 *          └─ -k : 자체 서명 인증서라 검증을 건너뛴다
 */
public class HttpsLoginServer {

    private static final int PORT = 8443;
    private static final String KEYSTORE = "keystore.p12";
    private static final char[] PASSWORD = "labpass".toCharArray();

    public static void main(String[] args) throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress(PORT), 0);

        // ⚠️ "keystore.p12" 는 "상대 경로" 다.
        //    실행 시점의 "현재 작업 디렉토리" 기준으로 찾는다. 파일 위치 기준이 아니다.
        File keystoreFile = new File(KEYSTORE);
        if (!keystoreFile.exists()) {
            System.err.println("keystore.p12 를 찾을 수 없습니다.");
            System.err.println("  현재 작업 디렉토리 : " + System.getProperty("user.dir"));
            System.err.println("  찾으려던 경로      : " + keystoreFile.getAbsolutePath());
            System.err.println();
            System.err.println("해결: 터미널에서 lab 디렉토리로 이동한 뒤 실행하세요.");
            System.err.println("  cd ~/Desktop/study/cs/network/lab");
            System.err.println("  java HttpsLoginServer.java");
            return;
        }

        // ── 여기가 HTTP 버전과의 유일한 차이 ──────────────────────
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(keystoreFile)) {
            keyStore.load(in, PASSWORD);          // 인증서 + 개인키를 읽는다
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, PASSWORD);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        // ────────────────────────────────────────────────────────

        server.createContext("/login", HttpsLoginServer::handleLogin);
        server.start();

        log("작업 디렉토리: " + System.getProperty("user.dir"));
        log("HTTPS 서버 시작 → https://127.0.0.1:" + PORT + "/login");
        log("✅ TLS 가 본문을 암호화한다. IP 와 포트는 여전히 평문이다.");
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        log("요청 수신(복호화 후): " + body);

        String response = """
                {"result":"ok","token":"eyJhbGciOiJIUzI1NiJ9.fake-token","user":"donghyun"}""";
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now().withNano(0) + "] [HTTPS:8443] " + msg);
    }
}
