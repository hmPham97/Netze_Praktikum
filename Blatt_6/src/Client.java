import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
    public static String address = "localhost";
    public static int port = 7999;
    public static void main(String[] args) {
        int length = 0;
        try {
            for(int i = 0; i < 3; i++) {
                DatagramSocket socket = new DatagramSocket();
                byte[] buf = new byte[1000];
                InetAddress adr = InetAddress.getByName(address);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, adr, port);
                length = length + buf.length;
                System.out.println(length);
                socket.send(packet);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
