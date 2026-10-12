import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

/**
 * [실습 3] HTTP 로그인 서버 — 평문이라는 것을 눈으로 보기 위한 도구
 *
 * JDK 에 내장된 HttpServer 를 쓴다. Gradle 도 Spring 도 필요 없다.
 * (Spring Boot 로 만들어도 선을 타고 가는 패킷은 똑같다)
 *
 * 실행:  java HttpLoginServer.java
 * 호출:  curl -X POST http://127.0.0.1:8080/login \
 *          -H "Content-Type: application/json" \
 *          -d '{"username":"donghyun","password":"MySecret1234"}'
 */
public class HttpLoginServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/login", HttpLoginServer::handleLogin);
        server.start();

        log("HTTP 서버 시작 → http://127.0.0.1:" + PORT + "/login");
        log("⚠️  암호화가 전혀 없다. TCP 위에 HTTP 텍스트가 그대로 흐른다.");
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        log("요청 수신: " + body);

        String response = """
                {"result":"ok","token":"eyJhbGciOiJIUzI1NiJ9.fake-token","user":"donghyun"}""";
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();

        log("응답 전송: " + response);
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now().withNano(0) + "] [HTTP:8080] " + msg);
    }
}
