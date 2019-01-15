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
    private byte[] sequencenumber = new byte[4];
    private byte[] check = new byte[8];
    private byte[] fromSender;
    private Checksum checksum;
    private int seq = 0;
    private Transition[][] transitions;
    private InetAddress fromServerAdr;
    private Reply reply;
    private Reply nameReply;
    private byte[] temporaryArray;
    private boolean firstPacket = true;
    private long time;
    private boolean timeStart = true;
    private long endTime;
    private int timeout = 10000;
    private long filesize = 0;

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
     *
     * @return
     */
    public String getFileName() {
        String theName;
        byte[] checkSeq;
        try {
            System.out.println(currentState);
            DatagramPacket name = new DatagramPacket(new byte[1400], 1400);
            socket.receive(name);
            if(timeStart) {
                time = System.currentTimeMillis();
                timeStart = false;
            }
           /* for (int i = 0; i < name.getLength(); i++) {
                System.out.println(name.getData()[i]);
            } */
            byte[] seqfromSender = new byte[4];
            System.arraycopy(name.getData(), 8, seqfromSender, 0, 4);

            // seq == 0
            System.out.println("seqFromSender has following value: " + seqfromSender[3]);
            if (seqfromSender[3] == seq) {
                CRC32 checkName = new CRC32();
                checkName.update(name.getData(), 8, name.getLength() - 8);
                ByteBuffer f = ByteBuffer.allocate(8);
                f.putLong(checkName.getValue());
                byte[] comp = f.array();

                for (int i = 0; i < comp.length; i++) {
                    System.out.println(comp[i] + "      " + name.getData()[i]);
                    if (comp[i] != name.getData()[i]) {
                        return "";
                        //theName = getFileName();
                        //return theName;
                    }
                }
            }


            ByteBuffer seqBuffer = ByteBuffer.allocate(4);
            seqBuffer.putInt(seq);

            checkSeq = seqBuffer.array();

            theName = new String(name.getData(), 12, name.getLength() - 12);
            fromServerAdr = name.getAddress();

            nameReply = new Reply(false, null);
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
            firstPacket = false;
            socket.setSoTimeout(timeout);
            fromServer = new DatagramPacket(buffer, buffer.length);
            socket.receive(fromServer);
            fromServerAdr = fromServer.getAddress();

            boolean checkSumIsCorrect = true;
            boolean alreadyReceived;

            byte[] from = fromServer.getData();

            check = Arrays.copyOfRange(from, 0, 8);
            sequencenumber = Arrays.copyOfRange(from, 8, 12);
            fromSender = Arrays.copyOfRange(from, 12, fromServer.getLength());
            for (int i = 0; i < sequencenumber.length; i++) {
                System.out.print(sequencenumber[i]);
            }
            System.out.println();
            checksum.reset();
            checksum.update(sequencenumber, 0, sequencenumber.length);
            checksum.update(fromSender, 0, fromSender.length);
            System.out.println("sequencenumber has the following value: " + sequencenumber[3]);
            ByteBuffer byteBufferForChecksum = ByteBuffer.allocate(8);
            byteBufferForChecksum.putLong(checksum.getValue());
            byte[] checkChecksum = byteBufferForChecksum.array();
            // check = von sender           checkChecksum ist von receiver
            System.out.println("Chceksum from calculating              checksum from sender");
            for (int i = 0; i < checkChecksum.length; i++) {
                System.out.println(checkChecksum[i] + "            " + check[i]);
                if (check[i] != checkChecksum[i]) {
                    // Checksumme stimmt nicht überein
                    System.out.println("Got a manipulated packet");
                    checkSumIsCorrect = false;
                    break;
                }
            }


            System.out.println();
            System.out.println(currentState);
            if (checkSumIsCorrect && sequencenumber[3] == 1) {
                firstPacket = false;
            }
            if (checkSumIsCorrect && firstPacket && sequencenumber[3] == 0) {
                System.out.println("We arrived here. IT SHOULD BE ONLY PRINTED ONCE!!! Except we got a corrupt packet");
                return getReply();
            } else {

                if (checkSumIsCorrect && seq == sequencenumber[3]) {
                    alreadyReceived = false;
                    //seq = (seq == 0) ? 1 : 0;
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
            endTime = System.currentTimeMillis() - timeout;
            time = (endTime - time) / 1000;
            System.out.println("size of file " + filesize);
            System.out.println("goodput " + filesize/time);
            System.out.println("time it took to send " + time);
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
        boolean state = reply1.getBoolean();
        int curSeq;
        System.out.println("state: " + state);
        System.out.println("firstpacket "  + firstPacket);
        System.out.println("my current seq " + seq);
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
        for(int i = 0; i < temporaryArray.length; i++) {
            System.out.println("inside the packet is at position: [" + i + "]    " + temporaryArray[i]);
        }
        try {
            if (state && !firstPacket) {
                filesize += reply1.getData().length;
                System.out.println("size of the reply1.getdata " + reply1.getData().length);
                System.out.println("size of file " + filesize);
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
        System.out.println("lenght of sendfilebyte2: " + sendFileByte2.length);
        byte[] copy = Arrays.copyOfRange(sendFileByte2, 0, sendFileByte2.length);
        int rnd = new Random().nextInt(sendFileByte2.length);
        System.out.println("random value which is replacing: " + rnd);
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

