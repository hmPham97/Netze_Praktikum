import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FileReceiver {

    enum State {
        WAIT_FOR_PACKET, RECEIVED_FROM_SERVER, DONE;
    }

    enum Doing {
        RECEIVE_FROM_SERVER, SEND_TO_SERVER;
    }

    private DatagramSocket socket;
    private DatagramPacket fromServer;
   // private InetAddress adr;
    private int port;
    private int length;
    private byte[] buffer = new byte[1028];
    private State currentState;

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




    abstract class Transition {
        abstract public State execute(Doing input);
    }
    class Got_A_Packet extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("Sending packet");
            return State.RECEIVED_FROM_SERVER;
        }
    }
    class Send_An_ACK extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("Got an ACK. Sending packet again");
            return State.WAIT_FOR_PACKET;
        }
    }
    class Timed_Out extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("No more packet available. Done with process");
            return State.DONE;
        }
    }
}

