import java.io.*;
import java.net.*;

public class Server extends Thread {

    private boolean running = true;
    private boolean isUDP = false;

    private byte[] buf = new byte[1400];
    private long size = 0;
    private long start = 0;
    private int received = 0;
    private int port = 7999;
    private long end;
    private double dsize;
    private double result;
    private int timeout = 5000;

    private DataInputStream dIn;
    private DatagramSocket socket;
    private ServerSocket tcpServer;


    private Server() {
        try {
            if (isUDP) {
                socket = new DatagramSocket(port);
            } else {
                tcpServer = new ServerSocket(port);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        if (isUDP) {
            UDP();
        } else {
            TCP();
        }
    }

    private void UDP() {
        try {
            socket.setSoTimeout(timeout); // set timeout
            start = System.currentTimeMillis();
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (running) {
                socket.receive(packet); // get package from client
                received++;
                size = size + packet.getLength(); // get its length
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
        end = System.currentTimeMillis() - timeout;
        end = (end - start) / 1000;
        dsize = size * 0.008;
        result = dsize / end;
        System.out.println("RECEIVED: " + received);
        System.out.println("Size in byte: " + size);
        System.out.println("SIZE IN kbit: " + dsize);
        System.out.println("TIME in sec: " + end);
        System.out.println(result + " in kbit/sec");
    }

    private void TCP() {
        try {
            tcpServer.setSoTimeout(timeout);
            Socket tcpSocket = tcpServer.accept();
            start = System.currentTimeMillis();
            dIn = new DataInputStream(tcpSocket.getInputStream());
            while (dIn.read() != -1) {
                System.out.println("available " + dIn.available());
                dIn.skip(1400);
                size += 1400;
                System.out.println(size);
                //dOut.write();
                //dOut.flush();
            }
            dIn.close();
        } catch (IOException e) {
            // e.printStackTrace();
        }
        end = System.currentTimeMillis() - timeout;
        end = (end - start) / 1000;
        dsize = size * 0.008;
        result = dsize / end;
        System.out.println("Size in byte: " + size);
        System.out.println("SIZE IN kbit: " + dsize);
        System.out.println("TIME in sec: " + end);
        System.out.println(result + " in kbit/sec");
    }

    public static void main(String[] args) {
        Server s = new Server();
        s.run();
    }

}
