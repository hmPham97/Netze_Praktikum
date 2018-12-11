import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSender {

    private DatagramSocket socket;
    private File f;
    private byte[] buf;
    private InetAddress adr;
    private int port;
    private int length;
    private int offset;
    private DatagramPacket packet;
    public FileSender(String file, String address) {
        try {
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
            this.offset = setOffset();
            socket.send(packet);
        } catch (IOException e) {
            // invoke endzustand
        }
    }

    public void waitForACK() {
        try {
            socket.setSoTimeout(10000);

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
