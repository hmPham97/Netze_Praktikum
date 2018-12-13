import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class FileReceiver {

    enum State {
        WAIT_FOR_PACKET, RECEIVED_FROM_SERVER, DONE;
    }

    enum Doing {
        RECEIVE_FROM_SERVER, SEND_TO_SERVER, TIMED_OUT;
    }

    private DatagramSocket socket;
    private DatagramPacket fromServer;
   // private InetAddress adr;
    private int port;
    private int length;
    private byte[] buffer = new byte[1028];
    private State currentState;

    private byte[] sequencenumber = new byte[4];
    private byte[] check = new byte[4];
    private byte[] fromSender;
    private Checksum checksum;
    private ByteBuffer byteBuffer;
    private  Transition[][] transitions;
    public FileReceiver() {
        try {
            currentState = State.WAIT_FOR_PACKET;
            transitions = new Transition[State.values().length][Doing.values().length];
            transitions[State.WAIT_FOR_PACKET.ordinal()][Doing.RECEIVE_FROM_SERVER.ordinal()] = new Got_A_Packet();
            transitions[State.RECEIVED_FROM_SERVER.ordinal()][Doing.SEND_TO_SERVER.ordinal()] = new Send_An_ACK();
            transitions[State.WAIT_FOR_PACKET.ordinal()][Doing.TIMED_OUT.ordinal()] = new Timed_Out();
            socket = new DatagramSocket(port);
            checksum = new CRC32();

        } catch (IOException e) {
        }
    }

    public void wait_for_packet() {
        try {
            fromServer = new DatagramPacket(buffer, length);
            socket.receive(fromServer);

            check = Arrays.copyOfRange(fromServer.getData(), 4, 7);
            fromSender = Arrays.copyOfRange(fromServer.getData(), 8, fromServer.getLength() - 1);
            checksum.update(fromSender);
            byteBuffer = ByteBuffer.allocate(4);
            byteBuffer.putLong(checksum.getValue());
            byte[] f = byteBuffer.array();
            for(int i = 0; i < f.length; i++) {
                if (check[i] != f[i]) {
                    // SOMETHING IS WRONG!!!!!!!!!!!!!!
                }
            }

        } catch (IOException e) {

        }
    }




    public void process(Doing input) {
        System.out.println("INFO Received " + input + " in state " + currentState);
        Transition trans = transitions[currentState.ordinal()][input.ordinal()];
        if (trans != null) {
            currentState = trans.execute(input);
        }
        System.out.println("current state: " + currentState);
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

