import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

/**
 * [실습 2] UDP 에코 서버 — TcpEchoServer 와 비교하며 볼 것
 *
 * 실행:  java UdpEchoServer.java
 * 접속:  nc -u 127.0.0.1 9001      ← -u 가 UDP 옵션
 */
public class UdpEchoServer {

    private static final int PORT = 9001;

    public static void main(String[] args) throws Exception {

        // ⚠️ TCP 와 다른 점 ①
        //    ServerSocket / Socket 구분이 없다. DatagramSocket 하나뿐이다.
        //    "연결"이 없으니 "연결을 받는 객체"도 필요 없다.
        try (DatagramSocket socket = new DatagramSocket(PORT)) {

            log("DatagramSocket 생성. 포트 " + PORT);
            log("TCP 와 다른 점: accept() 가 없다. 연결이라는 개념 자체가 없으니까.");
            log("접속:  nc -u 127.0.0.1 " + PORT);
            System.out.println();

            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // ⚠️ TCP 와 다른 점 ②
                //    accept() 가 아니라 receive(). 연결을 기다리는 게 아니라
                //    "패킷 하나"를 기다린다. 누가 보냈든 상관없이 받는다.
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength(),
                                        StandardCharsets.UTF_8).trim();

                // ⚠️ TCP 와 다른 점 ③
                //    상대 주소를 "패킷에서 꺼내야" 한다.
                //    연결이 없으니 서버가 상대를 기억하지 않는다.
                log("받음: \"" + msg + "\"  보낸이: "
                        + packet.getAddress().getHostAddress() + ":" + packet.getPort());

                byte[] reply = ("ECHO: " + msg + "\n").getBytes(StandardCharsets.UTF_8);
                socket.send(new DatagramPacket(
                        reply, reply.length, packet.getAddress(), packet.getPort()));

                log("보냄: \"ECHO: " + msg + "\"");
            }
        }
    }

    private static void log(String msg) {
        System.out.println("[" + LocalTime.now().withNano(0) + "] [UDP서버] " + msg);
    }
}
