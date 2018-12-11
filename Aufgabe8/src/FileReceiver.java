import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FileReceiver {

    private enum State {
        Wait_for_packet, Received_Packet, Done;
    }

    private DatagramSocket socket;
    private DatagramPacket fromServer;
   // private InetAddress adr;
    private int port;
    private int length;
    private byte[] buffer = new byte[1028];


    public FileReceiver() {
        try {
            socket = new DatagramSocket(port);
        } catch (IOException e) {
        }
    }

    public void wait_for_packet() {
        try {
            fromServer = new DatagramPacket(buffer, length);
            socket.receive(fromServer);
        } catch (IOException e) {

        }
    }
}
