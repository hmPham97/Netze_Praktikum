import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

/**
 * Sequenznummmer mitschicken.
 */
public class FileReceiverOO {

    enum State {
        WaitingForName, WaitForPacket, ReceivedFromServer, ReceivedFromServerName, Done;
    }

    enum Doing {
        ReceiveFromServer, SendToServer, TimedOut;
    }

    /**
     * Change macht probleme
     * chanceTwoChance problem : Wir kriegen bei receiver nicht 67 zurück
     */
    int chanceTwoTimes = 10;
    int chanceChange = 5;
    int chanceDelete = 5;
    String twoTimes = "twoTimes";
    String change = "change";
    String delete = "delete";
    String normal = "normal";
    ArrayList<String> arr = new ArrayList<String>();

    private DatagramSocket socket;
    private DatagramPacket fromServer;
    private Path path;
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
    private Reply reply;
    private DatagramPacket nameACK;
    private Reply nameReply;
    private byte[] checkSeq;
    private byte[] temporaryArray;
    private boolean firstPacket = true;

    public FileReceiverOO() {
        try {
            currentState = State.WaitingForName;
            transitions = new Transition[State.values().length][Doing.values().length];
            transitions[State.WaitingForName.ordinal()][Doing.ReceiveFromServer.ordinal()] = new GotName();
            transitions[State.ReceivedFromServerName.ordinal()][Doing.SendToServer.ordinal()] = new SendingACKForNamePacket();
            transitions[State.WaitForPacket.ordinal()][Doing.ReceiveFromServer.ordinal()] = new GotAPacket();
            transitions[State.ReceivedFromServer.ordinal()][Doing.SendToServer.ordinal()] = new SendAnACK();
            transitions[State.WaitForPacket.ordinal()][Doing.TimedOut.ordinal()] = new TimedOut();
            socket = new DatagramSocket(portToReceiver);
        } catch (IOException e) {
            System.err.println("Socket is already in use it seems");
        }
    }

    public void start() {
        initArr();
        checksum = new CRC32();
    }

    public void startPath(String name) {
        try {
            path = Paths.get(name);
            System.out.println("Name of the file we got " + name);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println();
        }
    }

    /**
     * aenderung evtl noetig
     *
     * @return
     */
    public String getFileName() {
        String theName;
        try {
            DatagramPacket name = new DatagramPacket(new byte[1400], 1400);
            socket.receive(name);
            byte[] seqfromSender = new byte[4];
            System.arraycopy(name.getData(), 8, seqfromSender, 0, 4);

            if (seqfromSender[3] == seq) {
                CRC32 checkName = new CRC32();
                checkName.update(name.getData(), 8, name.getLength() - 8);
                ByteBuffer f = ByteBuffer.allocate(8);
                f.putLong(checkName.getValue());
                byte[] comp = f.array();

                for (int i = 0; i < comp.length; i++) {
                    if (comp[i] != name.getData()[i]) {
                        return "";
                    }
                }
            }


            ByteBuffer seqBuffer = ByteBuffer.allocate(4);
            seqBuffer.putInt(seq);


            theName = new String(name.getData(), 12, name.getLength() - 12);
            fromServerAdr = name.getAddress();

            nameReply = new Reply(false, null);
            seq++;
            process(Doing.ReceiveFromServer);
            return theName;
        } catch (IOException e) {
            System.err.println("Failed to get name");
        }
        return "";
    }

    /**
     * CHECKEN OB SEQ > 0 & das SEQ# gleiche sind
     */
    public Reply waitingForPacket() {
        try {
            firstPacket = false;
            socket.setSoTimeout(10000);
            fromServer = new DatagramPacket(buffer, buffer.length);
            socket.receive(fromServer);
            fromServerAdr = fromServer.getAddress();

            boolean checkSumIsCorrect = true;
            boolean alreadyReceived;

            byte[] from = fromServer.getData();

            check = Arrays.copyOfRange(from, 0, 8);
            sequencenumber = Arrays.copyOfRange(from, 8, 12);
            fromSender = Arrays.copyOfRange(from, 12, fromServer.getLength());
            checksum.reset();
            checksum.update(sequencenumber, 0, sequencenumber.length);
            checksum.update(fromSender, 0, fromSender.length);
            ByteBuffer byteBufferForChecksum = ByteBuffer.allocate(8);
            byteBufferForChecksum.putLong(checksum.getValue());
            byte[] checkChecksum = byteBufferForChecksum.array();
            for (int i = 0; i < checkChecksum.length; i++) {
                if (check[i] != checkChecksum[i]) {
                    checkSumIsCorrect = false;
                    break;
                }
            }

            if (checkSumIsCorrect && sequencenumber[3] == 1) {
                firstPacket = false;
            }
            if (checkSumIsCorrect && firstPacket && sequencenumber[3] == 0) {
                return getReply();
            } else {

                if (checkSumIsCorrect && seq == sequencenumber[3]) {
                    alreadyReceived = false;
                } else {
                    alreadyReceived = true;
                }

                if (alreadyReceived) {
                    reply = new Reply(false, null);
                } else {
                    reply = new Reply(true, fromSender);
                }
                process(Doing.ReceiveFromServer);
            }
        } catch (IOException e) {
            System.out.println("Timed out.");
            process(Doing.TimedOut);
            socket.close();
        }
        return reply;
    }

    /**
     * SCHAUE WAS FÜR SEQ , WEnN 0 -> schreib nciht rein
     *
     * @param reply1
     */
    public void sendReply(Reply reply1) {
        boolean state = reply1.getBoolean();
        int curSeq;
        if (state) {
            curSeq = seq;
            seq = (seq == 0) ? 1 : 0;
        } else {
            curSeq = (seq == 0) ? 1 : 0;
        }
        if(firstPacket) {
            curSeq = 0;
        }
        temporaryArray = createPacket(curSeq);
        try {
            if (state && !firstPacket) {
                Files.write(path, reply1.getData(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            DatagramPacket sending = new DatagramPacket(temporaryArray, temporaryArray.length, fromServerAdr, portToSender);
            decideSend(sending);
            process(Doing.SendToServer);
        } catch (IOException e) {
            System.out.println("Failling to write");
        }
    }

    public void decideSend(DatagramPacket packet2) throws IOException {
        int rnd = new Random().nextInt(100);
        String current = arr.get(rnd);
        if (current.equalsIgnoreCase(normal)) {
            System.out.println("normal");
            socket.send(packet2);
        } else if (current.equalsIgnoreCase(twoTimes)) {
            System.out.println("twotimes");
            socket.send(packet2);
            socket.send(packet2);
        } else if (current.equalsIgnoreCase(change)) {
            System.out.println("change");
            byte wrongFileByte[] = manipulate(packet2.getData());
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
        System.out.println("current state: " + currentState + "\n");
    }

    private byte[] manipulate(byte[] sendFileByte2) {
        byte[] copy = Arrays.copyOfRange(sendFileByte2, 0, sendFileByte2.length);
        int rnd = new Random().nextInt(sendFileByte2.length);
        if (copy[rnd] == 0) {
            copy[rnd] = 1;
        } else {
            copy[rnd] = 0;
        }
        return sendFileByte2;
    }

    class Reply {
        boolean didNotGetAPacketBefore;
        byte[] data;

        public Reply(boolean didNotGetPacketBefore, byte[] data) {
            this.didNotGetAPacketBefore = didNotGetPacketBefore;
            this.data = data;
        }

        public boolean getBoolean() {
            return didNotGetAPacketBefore;
        }

        public byte[] getData() {
            return data;
        }
    }

    abstract class Transition {
        abstract public State execute(Doing input);
    }

    class GotName extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("Got file name. Sending ACK");
            return State.ReceivedFromServerName;
        }
    }

    class SendingACKForNamePacket extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("Send ACK. Waiting for packet");
            return State.WaitForPacket;
        }
    }

    class GotAPacket extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("Got a datapacket. Sending ACK");
            return State.ReceivedFromServer;
        }
    }

    class SendAnACK extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("Send an ACK. Waiting for packet");
            return State.WaitForPacket;
        }
    }

    class TimedOut extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("No more packet available. Done with process");
            return State.Done;
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

    public Reply getNameReply() {
        return nameReply;
    }

    public State getCurrentState() {
        return currentState;
    }

    public Reply getReply() {
        return reply;
    }

    private byte[] createPacket(int ack) {
        CRC32 checksum = new CRC32();
        checksum.reset();
        checksum.update(ack);
        long checksumValue = checksum.getValue();
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(checksumValue);
        byte[] packetACK = buffer.array();
        byte[] arraypacket;
        arraypacket = concat(ack, packetACK);
        return arraypacket;
    }


    private byte[] concat(int i, byte[] data1) {
        int data1Len = data1.length;
        //int data2Len = data.length;
        byte[] result = new byte[data1Len + 1];
        result[0] = (byte) i;
        System.arraycopy(data1, 0, result, 1, data1Len);
        //System.arraycopy(data2, 0, result, data1Len, data2Len);
        return result;
    }

}

