import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;

/**
 * [실습 1] TCP 에코 서버
 *
 * 받은 메시지를 그대로 돌려주는 가장 단순한 TCP 서버.
 * 3-way handshake 와 연결 상태 변화를 눈으로 보기 위한 도구다.
 *
 * 실행:  java TcpEchoServer.java          (Gradle 불필요. Java 11+ 단일 파일 실행)
 * 종료:  Ctrl + C
 */
public class TcpEchoServer {

    private static final int PORT = 9000;

    public static void main(String[] args) throws IOException {

        // ① ServerSocket 생성 = "이 포트로 오는 연결 요청을 받겠다"
        //    이 순간 서버는 LISTEN 상태가 된다.
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            log("ServerSocket 생성 완료. 포트 " + PORT + " → 지금부터 LISTEN 상태");
            log("다른 터미널에서 확인:  netstat -an | grep " + PORT);
            log("접속해보기:           nc localhost " + PORT);
            System.out.println();

            while (true) {
                log("accept() 호출 → 연결 요청을 기다리는 중... (여기서 멈춤)");

                // ② accept() 는 연결 요청이 올 때까지 "멈춰 있다"(블로킹).
                //    클라이언트가 접속하면 3-way handshake 가 끝난 뒤 반환된다.
                //    즉 이 줄이 반환됐다는 건 handshake 가 이미 완료됐다는 뜻.
                Socket client = serverSocket.accept();

                log("accept() 반환! → 3-way handshake 완료, ESTABLISHED 상태");
                log("  상대방 주소 : " + client.getRemoteSocketAddress());
                log("  내 쪽 주소  : " + client.getLocalSocketAddress());

                handle(client);
            }
        }
    }

    private static void handle(Socket client) {
        // try-with-resources: 블록이 끝나면 소켓이 닫히고 → 4-way 종료가 시작된다
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            String line;
            // ③ readLine() 도 블로킹. 상대가 한 줄 보낼 때까지 기다린다.
            //    상대가 연결을 끊으면 null 을 반환한다.
            while ((line = in.readLine()) != null) {
                log("받음: \"" + line + "\"");

                if ("bye".equalsIgnoreCase(line)) {
                    out.println("BYE");
                    log("bye 수신 → 서버가 연결을 끊는다");
                    break;
                }

                out.println("ECHO: " + line);   // 받은 걸 그대로 돌려줌
                log("보냄: \"ECHO: " + line + "\"");
            }

            if (line == null) {
                log("상대가 연결을 끊었다 (readLine 이 null 반환)");
            }

        } catch (IOException e) {
            log("오류: " + e.getMessage());
        }
        log("연결 종료. 다시 accept() 로 돌아간다");
        System.out.println();
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now().withNano(0) + "] [서버] " + msg);
    }
}
