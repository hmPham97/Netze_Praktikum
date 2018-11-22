import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server extends Thread{
    private DatagramSocket socket;
    private boolean running = true;
    private byte[] buf = new byte[1400];
    private int size = 0;
    public Server() {
        try {
            socket = new DatagramSocket(7999);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        long start = 0; // oder System.nanotime() ?
        //long end = 0;
        try {
            socket.setSoTimeout(5000); // set timeout
            start = System.currentTimeMillis();
            while(running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet); // get package from client
                size = size + packet.getLength(); // get its length
                //end = (end + System.currentTimeMillis()) / 1000;
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
        long end = System.currentTimeMillis() - 5000;
        end = end - start;
        end = end / 1000;
        long result = size / end;
        System.out.println(size);
        System.out.println(end);
        System.out.println(result);
    }

    public static void main(String[] args) {
        Server s = new Server();
        s.run();
    }

}
