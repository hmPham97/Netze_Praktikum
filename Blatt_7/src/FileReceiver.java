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
public class FileReceiver {

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
    private Reply reply;
    private DatagramPacket nameACK;
    private Reply nameReply;
    private byte[] checkSeq;
    private byte[] temporaryArray;

    public FileReceiver() {
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
            //String name = getFileName();
            //waitingForPacket();
    }

    public void startPath(String name) {
        try {
            path = Paths.get(name);
            System.out.println(name);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println();
        }
    }

    /**
     * aenderung evtl noetig
     * @return
     */
    public String getFileName() {
        String theName;
        try {
            System.out.println(currentState);
            DatagramPacket name = new DatagramPacket(new byte[1400], 1400);
            socket.receive(name);
           /* for (int i = 0; i < name.getLength(); i++) {
                System.out.println(name.getData()[i]);
            } */
            CRC32 checkName = new CRC32();
            checkName.update(name.getData(),8, name.getLength() - 8);
            ByteBuffer f = ByteBuffer.allocate(8);
            f.putLong(checkName.getValue());
            byte[] comp = f.array();
            for(int i = 0; i < comp.length; i++) {
                System.out.println(comp[i] +  "      " + name.getData()[i]);
                if (comp[i] != name.getData()[i]) {
                    return "";
                    //theName = getFileName();
                    //return theName;
                }
            }
            ByteBuffer seqBuffer = ByteBuffer.allocate(4);
            seqBuffer.putInt(0);
            checkSeq = seqBuffer.array();
            theName = new String(name.getData(), 12, name.getLength() - 12);
            fromServerAdr = name.getAddress();
            //nameACK = new DatagramPacket(new byte[]{1}, 1, fromServerAdr, portToSender);
            //socket.send(ack);
            nameReply = new Reply(1, false, null);
            ///decideSend(nameACK);
            System.out.println("Hello we are before process");
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
            socket.setSoTimeout(10000);
            fromServer = new DatagramPacket(buffer, buffer.length);
            socket.receive(fromServer);
            fromServerAdr = fromServer.getAddress();

            boolean inHere = true;
            boolean alreadyReceived = false;

            byte[] from = fromServer.getData();

            check = Arrays.copyOfRange(from, 0, 8);
            sequencenumber = Arrays.copyOfRange(from, 8, 12);
            fromSender = Arrays.copyOfRange(from, 12, fromServer.getLength());
            for(int i = 0; i < sequencenumber.length; i++) {
                System.out.print(sequencenumber[i]);
            }
            System.out.println();
            System.out.println(currentState);
            if(sequencenumber[0] == 0 && sequencenumber[1] == 0 && sequencenumber[2] == 0 && sequencenumber[3] == 0) {
                System.out.println("We arrived here. IT SHOULD BE ONLY PRINTED ONCE!!! Except we got a corrupt packet");
                return getReply();
            }
            else {
                checksum.reset();
                checksum.update(sequencenumber);
                checksum.update(fromSender);

                ByteBuffer byteBufferForChecksum = ByteBuffer.allocate(8);
                byteBufferForChecksum.putLong(checksum.getValue());
                byte[] checkChecksum = byteBufferForChecksum.array();
                ByteBuffer byteBufferForSequence = ByteBuffer.allocate(4);
                byteBufferForSequence.putInt(seq);
                checkSeq = byteBufferForSequence.array();
                seq++;
                System.out.println("Chceksum from calculating              checksum from sender");
                for (int i = 0; i < checkChecksum.length; i++) {
                    System.out.println(checkChecksum[i] + "            " + check[i]);
                    if (check[i] != checkChecksum[i]) {
                        // Checksumme stimmt nicht überein
                        System.out.println("Got a manipulated packet");
                        inHere = false;
                        seq--;
                        break;
                    }
                }

                if (inHere) {
                    System.out.println("seq from sender               seq from receiver");
                    for (int i = 0; i < checkSeq.length; i++) {
                        System.out.println("from sender:" + sequencenumber[i] + " from receiver:" + (checkSeq[i]));
                        if (sequencenumber[i] != checkSeq[i]) {
                            alreadyReceived = true;
                            seq--;
                            break;
                        }
                    }
                }

                if (inHere) {
                    if (alreadyReceived) {
                        reply = new Reply(1, false, null);
                    } else {
                        reply = new Reply(1, true, fromSender);
                    }
                } else {
                    reply = new Reply(0, false, null);
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
        System.out.println("i am in inside sendreply");
        temporaryArray = createPacket(reply1.getValue());
        /*sendToSender = new byte[]{0};
        if (reply1.getValue() == 1) {
            sendToSender[0] = 1;
        }
        */
        try {
            if (reply1.getBoolean() && seq != 0) {
                Files.write(path, reply1.getData(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            DatagramPacket sending = new DatagramPacket(temporaryArray, temporaryArray.length, fromServerAdr, portToSender);
            decideSend(sending);
            process(Doing.SendToServer);
            //waitingForPacket();
        } catch (IOException e) {
            System.out.println("Failling to write");
        }
    }

    public void decideSend(DatagramPacket packet2) throws IOException {
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
        System.out.println("current state: " + currentState + "\n");
    }

    private byte[] manipulate(byte[] sendFileByte2) {
        System.out.println(sendFileByte2.length);
        System.out.println(sendFileByte2[0] + "    " + sendFileByte2[1]);
        byte[] copy = Arrays.copyOfRange(sendFileByte2, 0, sendFileByte2.length);
        int rnd = new Random().nextInt(sendFileByte2.length);
        if(copy[rnd] == 0) {
            copy[rnd] = 1;
        }
        else {
            copy[rnd] = 0;
        }
        return sendFileByte2;
    }

    class Reply {
        int i;
        boolean didNotGetAPacketBefore;
        byte[] data;
        public Reply(int i, boolean didNotGetPacketBefore, byte[] data) {
            this.i = i;
            this.didNotGetAPacketBefore = didNotGetPacketBefore;
            this.data = data;
        }
        public int getValue() {
            return i;
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

    class GotName extends  Transition {
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

    public DatagramPacket getNameACK() {
        return nameACK;
    }

    public Reply getReply() {
        return reply;
    }

    public byte[] getCheckSeq() {
        return checkSeq;
    }

    private byte[] createPacket(int ack) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(ack);
        byte[] packetACK = buffer.array();
        byte[] arraypacket;
        arraypacket = concat(getCheckSeq(), packetACK);
        return arraypacket;
    }

    private byte[] concat(byte[] data1, byte[] data2) {
        int data1Len = data1.length;
        int data2Len = data2.length;
        byte[] result = new byte[data1Len + data2Len];
        System.arraycopy(data1, 0, result, 0, data1Len);
        System.arraycopy(data2, 0, result, data1Len, data2Len);
        return result;
    }
}

