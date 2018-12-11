import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSender {

    private enum State {
        SEND, WAIT_FOR_ACK, DONE;
    }


    private DatagramSocket socket;
    private File f;
    private InetAddress adr;
    private int port;

    // SENDER NECESSARY
    private byte[] buf;
    private int length;
    private int offset;

    private DatagramPacket packet;

    // RECEIVED FROM RECEIVER
    private DatagramPacket fromServer;
    private byte[] fromServerBuf = new byte[1028];

    private State state;

    public FileSender(String file, String address) {
        try {
            state = State.SEND;
            socket = new DatagramSocket();
            File f = new File(file);
            Path path = Paths.get(f.getAbsolutePath());
            buf = Files.readAllBytes(path);
            adr = InetAddress.getByName(address);
        } catch (IOException e) {
            System.err.println("Socket Exception");
        }
    }

    public void send(int offset) {
        try{
           // socket.setSoTimeout(10000);
            packet = new DatagramPacket(buf, offset, length, adr, port);
            if(offset == buf.length) {
                throw new IOException();
            }
            this.offset = setOffset();
            socket.send(packet);
            state = State.WAIT_FOR_ACK;
        } catch (IOException e) {
            state = State.DONE;
        }
    }

    public void waitForACK() {
        try {
            socket.setSoTimeout(10000);
            state = State.SEND;
            fromServer = new DatagramPacket(fromServerBuf, fromServerBuf.length);
            socket.receive(fromServer);
        } catch (IOException e) {
            send(getOffset());
        }
    }

    private int getOffset() {
        return offset;
    }

    private int setOffset() {
        return offset + length;
    }

    public int getLength() {
        return length;
    }

    public DatagramPacket getPacket() {
        return packet;
    }
}
