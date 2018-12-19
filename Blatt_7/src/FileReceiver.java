import javax.xml.crypto.Data;
import java.io.*;
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

    int chanceTwoTimes = 0;
    int chanceChange = 0;
    int chanceDelete = 0;
    String twoTimes = "twoTimes";
    String change = "change";
    String delete = "delete";
    String normal = "normal";
    ArrayList<String> arr = new ArrayList<String>();

    private DatagramSocket socket;
    private DatagramPacket fromServer;
    private byte[] sendToSender;
    private Path path;
    private String adr = "localhost";
    private int portToSender = 9001;
    private int portToReceiver = 9002;
    private byte[] buffer = new byte[1400];
    private State currentState;
    private InetAddress address;
    private byte[] sequencenumber = new byte[4];
    private byte[] check = new byte[8];
    private byte[] fromSender;
    private Checksum checksum;
    private int seq = 0;
    private Transition[][] transitions;
    private InetAddress fromServerAdr;
    private File writeFileInHere;

    public FileReceiver() {
        try {
            initArr();
            currentState = State.WAIT_FOR_PACKET;
            transitions = new Transition[State.values().length][Doing.values().length];
            transitions[State.WAIT_FOR_PACKET.ordinal()][Doing.RECEIVE_FROM_SERVER.ordinal()] = new Got_A_Packet();
            transitions[State.RECEIVED_FROM_SERVER.ordinal()][Doing.SEND_TO_SERVER.ordinal()] = new Send_An_ACK();
            transitions[State.WAIT_FOR_PACKET.ordinal()][Doing.TIMED_OUT.ordinal()] = new Timed_Out();
            socket = new DatagramSocket(portToReceiver);
            String name = getFileName();
            path = Paths.get(name);
            System.out.println(name);
            Files.deleteIfExists(path);
            //path = Paths.get("").toAbsolutePath();
            //System.out.println(path.toString());
            //w-riteFileInHere = new File(path.toString() + "\\tmp.png");
            checksum = new CRC32();
            wait_for_packet();
        } catch (IOException e) {
            System.err.println("There was some error in the constructor");
        }
    }

    private String getFileName() {
        String theName;
        try {
            DatagramPacket name = new DatagramPacket(new byte[1400], 1400);
            socket.receive(name);
            CRC32 checkName = new CRC32();
            checkName.update(name.getData(),8, name.getLength() - 8);
            ByteBuffer f = ByteBuffer.allocate(8);
            f.putLong(checkName.getValue());
            byte[] comp = f.array();
            for(int i = 0; i < comp.length; i++) {
                if (comp[i] != name.getData()[i]) {
                    theName = getFileName();
                    return theName;
                }
            }
            theName = new String(name.getData(), 8, name.getLength() - 8);
            fromServerAdr = name.getAddress();
            DatagramPacket ack = new DatagramPacket(new byte[]{1}, 1, fromServerAdr, portToSender);
            socket.send(ack);
            return theName;
        } catch (IOException e) {
            System.err.println("Failed to get name");
        }
        return "";
    }

    private void wait_for_packet() {
        try {
            socket.setSoTimeout(100000);
            fromServer = new DatagramPacket(buffer, buffer.length);
            socket.receive(fromServer);
            fromServerAdr = fromServer.getAddress();
            System.out.println(fromServer.getLength());
            boolean inHere = true;
            boolean alreadyReceived = false;
            byte[] from = fromServer.getData();

            check = Arrays.copyOfRange(from, 0, 8);
            sequencenumber = Arrays.copyOfRange(from, 8, 12);
            fromSender = Arrays.copyOfRange(from, 12, fromServer.getLength());
            checksum.reset();
            checksum.update(sequencenumber);
            checksum.update(fromSender);
            ByteBuffer byteBufferForChecksum = ByteBuffer.allocate(8);
            byteBufferForChecksum.putLong(checksum.getValue());
            byte[] checkChecksum = byteBufferForChecksum.array();
            ByteBuffer byteBufferForSequence = ByteBuffer.allocate(4);
            byteBufferForSequence.putInt(seq);
            byte[] checkSeq = byteBufferForSequence.array();


            seq++;
            for (int i = 0; i < checkChecksum.length; i++) {
                System.out.println(checkChecksum[i] + "            " + check[i]);
                if (check[i] != checkChecksum[i]) {
                    // Checksumme stimmt nicht überein
                    inHere = false;
                    seq--;
                    break;
                }
            }

            if (inHere) {
                for (int i = 0; i < checkSeq.length; i++) {
                    System.out.println(sequencenumber[i] + " " + (checkSeq[i]));
                    if (sequencenumber[i] != checkSeq[i]) {
                        alreadyReceived = true;
                        seq--;
                        break;
                    }
                }
            }
            System.out.println("inhere = " + inHere);
            System.out.println("alreadyreceived = " + alreadyReceived);
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
        sendToSender = new byte[]{0};
        if (i == 1) {
            sendToSender[0] = 1;
        }
        try {
            if (bool) {
                Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            DatagramPacket sending = new DatagramPacket(sendToSender, sendToSender.length, fromServerAdr, portToSender);
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
        System.out.println(arr.get(rnd));
        if (current.equalsIgnoreCase(normal)) {
            socket.send(packet2);
        } else if (current.equalsIgnoreCase(twoTimes)) {
            socket.send(packet2);
            socket.send(packet2);
        } else if (current.equalsIgnoreCase(change)) {
            byte wrongFileByte[] = manipulate(sendToSender);
            DatagramPacket wrongPacket = new DatagramPacket(wrongFileByte, wrongFileByte.length, fromServerAdr, portToSender);
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
        sendFileByte2[0] = (byte) (sendFileByte2[0] << 1);
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

