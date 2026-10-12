import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

/**
 * [실습 2-2] UDP 패킷 유실 재현
 *
 * 번호가 붙은 패킷을 대량으로 보내고, 몇 개가 도착했는지 센다.
 * TCP 라면 전부 도착한다. UDP 는 그렇지 않다.
 *
 * 실행:  java UdpLossTest.java            (기본 20000개)
 *        java UdpLossTest.java 50000      (개수 지정)
 */
public class UdpLossTest {

    private static final int PORT = 9002;

    public static void main(String[] args) throws Exception {
        int total = args.length > 0 ? Integer.parseInt(args[0]) : 20000;

        BitSet received = new BitSet(total + 1);

        // --- 수신 스레드 ---
        DatagramSocket server = new DatagramSocket(PORT);
        server.setReceiveBufferSize(8 * 1024);   // 수신 버퍼를 일부러 작게
        server.setSoTimeout(1500);               // 1.5초 동안 아무것도 안 오면 종료

        Thread receiver = new Thread(() -> {
            byte[] buf = new byte[64];
            while (true) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    server.receive(p);
                    int seq = Integer.parseInt(
                            new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8));
                    received.set(seq);
                } catch (Exception e) {
                    break;   // 타임아웃 = 더 이상 안 옴
                }
            }
        });
        receiver.start();

        // --- 송신 ---
        System.out.println("패킷 " + total + "개를 최대한 빠르게 전송합니다...");
        long start = System.currentTimeMillis();

        try (DatagramSocket client = new DatagramSocket()) {
            InetAddress addr = InetAddress.getByName("127.0.0.1");
            for (int i = 1; i <= total; i++) {
                byte[] data = String.valueOf(i).getBytes(StandardCharsets.UTF_8);
                client.send(new DatagramPacket(data, data.length, addr, PORT));
                // ⚠️ send() 는 "보냈다"만 알려준다. "도착했다"가 아니다.
            }
        }
        long sendMs = System.currentTimeMillis() - start;

        receiver.join();
        server.close();

        int got = received.cardinality();
        int lost = total - got;

        System.out.println();
        System.out.println("┌──────────────────────────────────────┐");
        System.out.printf ("│  보낸 패킷   : %,10d              │%n", total);
        System.out.printf ("│  받은 패킷   : %,10d              │%n", got);
        System.out.printf ("│  유실        : %,10d  (%.2f%%)    │%n", lost, lost * 100.0 / total);
        System.out.printf ("│  전송 소요   : %,10d ms           │%n", sendMs);
        System.out.println("└──────────────────────────────────────┘");

        if (lost > 0) {
            System.out.print("\n유실된 번호 예시: ");
            int shown = 0;
            for (int i = 1; i <= total && shown < 15; i++) {
                if (!received.get(i)) { System.out.print(i + " "); shown++; }
            }
            System.out.println("...");
            System.out.println("\n→ send() 는 전부 성공했는데 받은 건 " + got + "개다.");
            System.out.println("  UDP 는 '보냈다'만 보장한다. '도착했다'는 보장하지 않는다.");
        } else {
            System.out.println("\n이번엔 유실이 없었다. 개수를 늘려서 다시 시도해보자:");
            System.out.println("  java UdpLossTest.java 100000");
        }
    }
}
