import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.zip.CRC32;

public class FileSender {

    enum State {
        SendingName, WaitingForACKNamePacket, SendPacket, SendPacketAgain, WaitingForACK, Done;
    }

    enum Doing {
        SendingToReceiver, Resend, ReceivedACKFromReceiver, FoundNothingLeftToSend;
    }

    /**
     * Muessen noch chanceChange FIXEN
     */

    public int manipulated;
    int chanceTwoTimes =10;
    int chanceChange = 10;
    int chanceDelete = 0;
    String twoTimes = "twoTimes";
    String change = "change";
    String delete = "delete";
    String normal = "normal";
    ArrayList<String> arr = new ArrayList<String>();

    private int endPacketSize = 1400;
    private DatagramSocket socket;
    private File f;
    private String fileName;
    private InetAddress adr;
    private final int port = 9001;
    private final int portSendingToReceiver = 9002;
    private int sequenceNumber = 0;

    // SENDER NECESSARY
    private byte[] buf;
    private int length = 1388;
    private int offset = 0;
    private DatagramPacket packet;
    private DatagramPacket savepacket;
    // RECEIVED FROM RECEIVER
    private DatagramPacket fromServer;
    private byte[] fromServerBuf = new byte[8];
    private State currentState;
    private Transition[][] transitions;

    private int receivedFromServer;
    private byte[] sendFileByte;
    private byte[] arrayToCheckSeq;


    private DatagramPacket containsName;
    private String nameOfFile;

    /**
     * Start the Automat for Sender. Starts automat in SendPacket state. Also start up socket for Sender.
     *
     * @param fileName The file which will be used.
     * @param address  The destination where the file will be send to.
     */
    public FileSender(String fileName, String address) {
        try {
            this.fileName = fileName;
            currentState = State.SendingName;
            transitions = new Transition[State.values().length][Doing.values().length];

            // SENDE NAME -> WARTE AUF EINE ANTWORT FÜR NAME
            transitions[State.SendingName.ordinal()][Doing.SendingToReceiver.ordinal()] = new SendingNameWaitingForReply();
            // ANTOWRT ERHALTEN -> GEHE ZUM SENDPACKET STATUS. WENN TIMEOUT SENDE PACKET WIEDER
            transitions[State.WaitingForACKNamePacket.ordinal()][Doing.ReceivedACKFromReceiver.ordinal()] = new Wait_To_Send();
            // SENDET PACKET -> WARTET AUF ACK VON RECEIVER
            transitions[State.SendPacket.ordinal()][Doing.SendingToReceiver.ordinal()] = new Send_To_Wait();
            // ACK ERHALTEN VON RECEIVER -> GEHE WIEDER AUF SENDE PACKET STATUS
            transitions[State.WaitingForACK.ordinal()][Doing.ReceivedACKFromReceiver.ordinal()] = new Wait_To_Send();
            // VERSUCHT PACKET ZU SENDEN -> FINDET NICHTS MEHR -> BEENDE PROGRAMM
            transitions[State.SendPacket.ordinal()][Doing.FoundNothingLeftToSend.ordinal()] = new Send_To_Close();

            f = new File(fileName);
            adr = InetAddress.getByName(address);
            socket = new DatagramSocket(port);
        } catch (IOException e) {
            System.err.println("Socket Exception");
        }
    }

    public void start() {
        try {
            initArr();
            nameOfFile = fileName.substring(fileName.lastIndexOf("\\") + 1);
            Path path = Paths.get(f.getAbsolutePath());
            buf = Files.readAllBytes(path);
            //sendFileName(nameOfFile.getBytes());%
            //send(offset);
        } catch (IOException e) {

        }
    }

    /**
     * CREATE PACKET HIER AUFRUFEN
     */
    public void sendFileName(byte[] name) {
        if (currentState == State.SendingName) {
            ByteBuffer seqBuffer = ByteBuffer.allocate(4);
            seqBuffer.putInt(sequenceNumber);
            ByteBuffer f = ByteBuffer.allocate(8);
            byte[] part = new byte[name.length + seqBuffer.array().length];
            byte[] whole = new byte[f.array().length + part.length];
            System.arraycopy(seqBuffer.array(), 0, part, 0, seqBuffer.array().length);
            System.arraycopy(name, 0, part, seqBuffer.array().length, name.length);
            CRC32 check = new CRC32();
            check.reset();
            check.update(part);
            long v = check.getValue();
            f.putLong(v);
            System.arraycopy(f.array(), 0, whole, 0, f.array().length);
            System.arraycopy(part, 0, whole, f.array().length, part.length);

            containsName = new DatagramPacket(whole, whole.length, adr, portSendingToReceiver);
            DatagramPacket gotACK = new DatagramPacket(new byte[10], 10);
            //socket.send(containsName);
            //process(Doing.SendingToReceiver);
            // muss in WAITFORACK
            // aendern
            //socket.receive(gotACK);
            //
            //System.out.println(gotACK.getData()[0]);
/*
            if (gotACK.getData()[0] == 0) {
                sendFileName(name);
            }
            */
            //
        }
    }

    /**
     * Sends the packet. Puts current state to WaitingForACK;
     *
     * @param offset
     */
    public void send(int offset) {
        try {
            if (currentState == State.SendingName) {
                decideSend(getContainsName());
                sequenceNumber++;
                process(Doing.SendingToReceiver);
            } else if (currentState == State.WaitingForACKNamePacket) {
                decideSend(getContainsName());
            } else if (currentState == State.SendPacket) {
                socket.setSoTimeout(10000);
                sendFileByte = createPacket();
                packet = new DatagramPacket(sendFileByte, sendFileByte.length, adr, portSendingToReceiver);
                savePacket(packet);
                if (offset >= buf.length) {
                    //process(Doing.FoundNothingLeftToSend);
                    socket.close();
                    process(Doing.FoundNothingLeftToSend);
                } else if (currentState == State.SendPacket) {
                    decideSend(packet);
                    sequenceNumber++;
                    process(Doing.SendingToReceiver);
                }
                //AENDERN HIER
                //waitForACK();
            } else if (currentState == State.WaitingForACK) {
                System.out.println(getPacket().getData()[manipulated]);
                decideSend(getPacket());
                //AENDERN HIER
                //waitForACK();
            }
        } catch (SocketException e) {
            System.err.println("No receiver connected with sender");
        } catch (IOException | TimeoutException e) {
            System.err.println("bye");
            process(Doing.FoundNothingLeftToSend);
            socket.close();
        }
    }

    /**
     * holt sich aus dem array arr der groesse 100 einen zufaelligen string. je nach string wird das paket manipuliert und versendet,
     * nicht versendet, versendet oder doppelt versendet
     *
     * @param packet2
     * @throws IOException
     */
    private void decideSend(DatagramPacket packet2) throws IOException {
        int rnd = new Random().nextInt(100);
        String current = arr.get(rnd);
        if (current.equalsIgnoreCase(normal)) {
            System.out.println(normal);
            socket.send(packet2);
        } else if (current.equalsIgnoreCase(twoTimes)) {
            System.out.println("two times");
            socket.send(packet2);
            socket.send(packet2);
        } else if (current.equalsIgnoreCase(change)) {
            System.out.println("change");
            byte[] wrongFileByte = manipulate(sendFileByte);
            DatagramPacket wrongPacket = new DatagramPacket(wrongFileByte, wrongFileByte.length, adr, portSendingToReceiver);
            socket.send(wrongPacket);
        } else if (current.equalsIgnoreCase(delete)) {
            System.out.println("delete");
        }
    }

    /**
     * manipuliert einzelne bits von jedem Byte (12-1400) des Byte Arrays. Byte 0-11 enthaelt die Sequenznummer und Checksumme.
     * Diese sollen nicht manipuliert werden.
     *
     * @param sendFileByte2
     * @return
     */
    private byte[] manipulate(byte[] sendFileByte2) {
        byte[] result = Arrays.copyOfRange(sendFileByte2, 0, sendFileByte2.length);
        System.out.println(result.length);
        int rnd = new Random().nextInt(endPacketSize);
        byte i = result[rnd];
        if (i != 0) {
            result[rnd] = 0;
        } else {
            result[rnd] = 1;
        }
        manipulatedBit(rnd);
        return result;
    }

    /**
     * Sender wartet auf eine Antwort des Receivers
     */
    public void waitForACK() {
        if (currentState == State.WaitingForACK || currentState == State.WaitingForACKNamePacket) {
            try {
                socket.setSoTimeout(500);
                fromServer = new DatagramPacket(fromServerBuf, fromServerBuf.length);
                socket.receive(fromServer);
                for(int i = 0; i < fromServer.getLength(); i++) {
                    System.out.println(fromServer.getData()[i]);
                }
                receivedFromServer = fromServer.getData()[7];
                // hier nochmal sequenznummer vergleichen!!!! die sequenznummer von RECEIVER MUSS IMMER EINS KLEINER SEIN ALS DIE SEQUENZNUMMER DIE IN SENDER GERADE IST
                // SOLL AUCH NE CHECKSUMMER GESENDET WERDEN?
                if (currentState == State.WaitingForACK) {
                    boolean checking = true;
                    byte[] checkSeq = Arrays.copyOfRange(fromServer.getData(), 0, 3);
                    for (int i = 0; i < checkSeq.length; i++) {
                        if (checkSeq[i] != getArrayToCheckSeq()[i]) {
                            checking = false;
                            break;
                        }
                    }
                    if (checking) {
                        if (receivedFromServer == 1) {
                            offset = offset + length;
                            process(Doing.ReceivedACKFromReceiver);
                        }
                    }
                }
                // kommt hier nur rein wenn WaitingForACKNamePacket
                else {
                    if (receivedFromServer == 1) {
                        if (fromServer.getData()[0] == 0 || fromServer.getData()[1] == 0 || fromServer.getData()[2] == 0 || fromServer.getData()[3] == 0)
                            process(Doing.ReceivedACKFromReceiver);
                    }
                }
            } catch (SocketTimeoutException e) {
                System.err.println("timedout");
            } catch (IOException e) {
                System.out.println("Receive failed");
            }
        }
    }

    /**
     * erzeugt das Paket das versendet wird. Byte 0-7 = Checksumme. Byte 8-11 = Sequenznummer. Byte 12-1400 = Daten.
     *
     * @return
     */
    private byte[] createPacket() throws TimeoutException {
        byte byteArray[];
        if ((buf.length - offset) >= 1388) {
            byteArray = Arrays.copyOfRange(buf, offset, offset + length);
        } else if (offset >= buf.length) {
            throw new TimeoutException();
        } else {
            byteArray = Arrays.copyOfRange(buf, offset, buf.length);
        }
        byteArray = appendSequenceNumber(byteArray);
        byteArray = appendChecksum(byteArray);
        return byteArray;
    }

    /**
     * an das byte Array data wird vorne die Sequenznummer angehaengt (4 Byte).
     *
     * @param data
     * @return
     */
    private byte[] appendSequenceNumber(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(sequenceNumber);
        byte[] packetSequence = buffer.array();
        data = concat(packetSequence, data);
        arrayToCheckSeq = Arrays.copyOfRange(packetSequence, 0, packetSequence.length);
        return data;
    }

    /**
     * an das byte Array data wird vorne die Checksumme angehaengt (4 Byte).
     *
     * @param data
     * @return
     */
    private byte[] appendChecksum(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        ByteBuffer buff = ByteBuffer.allocate(8);
        buff.putLong(crc.getValue());
        byte[] checksum = buff.array();
        data = concat(checksum, data);
        return data;
    }

    /**
     * data1 wird vor data2 gehaengt (in einem neuen byte array result).
     *
     * @param data1
     * @param data2
     * @return
     */
    private byte[] concat(byte[] data1, byte[] data2) {
        int data1Len = data1.length;
        int data2Len = data2.length;
        byte[] result = new byte[data1Len + data2Len];
        System.arraycopy(data1, 0, result, 0, data1Len);
        System.arraycopy(data2, 0, result, data1Len, data2Len);
        return result;
    }


    /**
     * wird benoetigt um einstellige wahrcheinlichkeiten von 4 ereignissen zu erzeugen.
     */
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

    private void process(Doing input) {
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

    class SendingNameWaitingForReply extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("Sending to Receiver filename");
            return State.WaitingForACKNamePacket;
        }
    }

    class Send_To_Wait extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("SendingToReceiver");
            return State.WaitingForACK;
        }
    }

    class Wait_To_Send extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("Got an ACK. Sending packet again");
            return State.SendPacket;
        }
    }

    class Send_To_Close extends Transition {
        @Override
        public State execute(Doing input) {
            System.out.println("No more packet available. Done with process");
            return State.Done;
        }
    }

    private byte[] getArrayToCheckSeq() {
        return arrayToCheckSeq;
    }

    private void manipulatedBit(int manipulatedBit) {
        manipulated = manipulatedBit;
    }

    private void savePacket(DatagramPacket packet1) {
        this.savepacket = packet1;
    }

    private DatagramPacket getPacket() {
        return savepacket;
    }

    public State getCurrentState() {
        return currentState;
    }

    public byte[] getFileName() {
        return buf;
    }

    public DatagramPacket getContainsName() {
        return containsName;
    }

    public int getOffset() {
        return offset;
    }

    public String getNameOfFile() {
        return nameOfFile;
    }
}