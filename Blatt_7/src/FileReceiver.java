import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class FileReceiver {

    enum State {
        WAIT_FOR_PACKET, RECEIVED_FROM_SERVER, DONE;
    }

    enum Doing {
        RECEIVE_FROM_SERVER, SEND_TO_SERVER, TIMED_OUT;
    }

    int chanceTwoTimes = 5;
    int chanceChange = 5;
    int chanceDelete = 5;
    String twoTimes = "twoTimes";
    String change = "change";
    String delete = "delete";
    String normal = "normal";
    ArrayList<String> arr = new ArrayList<String>();

    private DatagramSocket socket;
    private DatagramPacket fromServer;
    private DatagramPacket toSender;
    private byte[] sendToSender;
    private Path path;
    private String adr = "localhost";
    private int port = 9001;
    private byte[] buffer = new byte[1400];
    private State currentState;
    private InetAddress address;
    private byte[] sequencenumber = new byte[4];
    private byte[] check = new byte[8];
    private byte[] fromSender;
    private Checksum checksum;
    private int seq = 0;
    private Transition[][] transitions;

    public FileReceiver() {
        try {
            initArr();
            currentState = State.WAIT_FOR_PACKET;
            transitions = new Transition[State.values().length][Doing.values().length];
            transitions[State.WAIT_FOR_PACKET.ordinal()][Doing.RECEIVE_FROM_SERVER.ordinal()] = new Got_A_Packet();
            transitions[State.RECEIVED_FROM_SERVER.ordinal()][Doing.SEND_TO_SERVER.ordinal()] = new Send_An_ACK();
            transitions[State.WAIT_FOR_PACKET.ordinal()][Doing.TIMED_OUT.ordinal()] = new Timed_Out();
            socket = new DatagramSocket(port);
            checksum = new CRC32();
            wait_for_packet();
        } catch (IOException e) {
            System.err.println("There was some error in the constructor");
        }
    }

    private void wait_for_packet() {
        try {
            fromServer = new DatagramPacket(buffer, buffer.length);
            socket.receive(fromServer);
            boolean inHere = true;
            boolean alreadyReceived = false;
            byte[] from = fromServer.getData();
            System.out.println(from.length);
            for(int i = 0; i < from.length; i++) {
                System.out.println(from[i]);
            }
            check = Arrays.copyOfRange(from, 0, 8);
            sequencenumber = Arrays.copyOfRange(from, 8, 12);
            fromSender = Arrays.copyOfRange(from, 12, from.length);
            checksum.update(fromSender);
            seq++;
            ByteBuffer byteBufferForChecksum = ByteBuffer.allocate(8);
            byteBufferForChecksum.putLong(checksum.getValue());
            byte[] checkChecksum = byteBufferForChecksum.array();
            ByteBuffer byteBufferForSequence = ByteBuffer.allocate(4);
            byteBufferForSequence.putInt(seq);
            byte[] checkSeq = byteBufferForSequence.array();
            for (int i = 0; i < checkChecksum.length; i++) {
                if (check[i] != checkChecksum[i]) {
                    inHere = false;
                }
            }
            if (inHere) {
                for (int i = 0; i < checkSeq.length; i++) {
                    if (sequencenumber[i] != checkSeq[i]) {
                        alreadyReceived = true;
                        seq--;
                    }
                }
            }
            process(Doing.RECEIVE_FROM_SERVER);
            if (inHere) {
                if (alreadyReceived) {
                    sendReply(1, false, null);
                } else {
                    sendReply(1, true, fromSender);
                }
            } else {
                sendReply(0, false, null);
            }

        } catch (IOException e) {
            System.out.println("Timed out.");
            process(Doing.TIMED_OUT);
            socket.close();
        }
    }

    private void sendReply(int i, boolean bool, byte[] data) {
        System.out.println("i am in inside sendreply");
        sendToSender = new byte[1];
        if (i == 1) {
            sendToSender[0] = 1;
        }
        try {
            toSender = new DatagramPacket(sendToSender, sendToSender.length, address, port);
            if(bool) {
                path = Paths.get("input/tmp.txt");
                Files.write(path, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND);
            }
            DatagramPacket sending = new DatagramPacket(sendToSender, sendToSender.length);
            decideSend(sending);
            process(Doing.SEND_TO_SERVER);
            wait_for_packet();
        } catch (IOException e) {
            System.out.println("Failling to write");
        }
    }

    private void decideSend(DatagramPacket packet2) throws IOException {
        int rnd = new Random().nextInt(100);
        String current = arr.get(rnd);

        if (current.equalsIgnoreCase(normal)) {
            socket.send(packet2);
        }
        else if (current.equalsIgnoreCase(twoTimes)) {
            socket.send(packet2);
            socket.send(packet2);
        }
        else if (current.equalsIgnoreCase(change)) {
            byte wrongFileByte[] = manipulate(sendToSender);
            DatagramPacket wrongPacket = new DatagramPacket(wrongFileByte , wrongFileByte.length);
            socket.send(wrongPacket);
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

    private byte[] manipulate(byte[] sendFileByte2) {
        for(int i = 0; i < sendFileByte2.length; i++) {
            sendFileByte2[i] = (byte)(sendFileByte2[i] << 2);
        }
        return sendFileByte2;
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

    private void initArr() {
        for (int i = 0; i < chanceTwoTimes; i++) {
            arr.add(twoTimes);
        }
        for (int j = 0; j < chanceChange; j++) {
            arr.add(change);
        }
        for (int k = 0; k < chanceDelete; k++) {
            arr.add(delete);
        }
        for (int z = 0; z < 100 - chanceTwoTimes - chanceChange - chanceDelete; z++) {
            arr.add(normal);
        }
        Collections.shuffle(arr);
    }

}

