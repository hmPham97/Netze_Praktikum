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

//    private DataInputStream dIn;
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
            end = System.currentTimeMillis() - timeout;
            end = (end - start) / 1000;
            dsize = size * 0.008;
            result = dsize / end;
            System.out.println("RECEIVED: " + received);
            System.out.println("Size in byte: " + size);
            System.out.println("SIZE IN kbit: " + dsize);
            System.out.println("TIME in sec: " + end);
            System.out.println(result + " in kbit/sec");        }
    }

    private void TCP() {
    	int current = 0;
        try {
            tcpServer.setSoTimeout(timeout);
            Socket tcpSocket = tcpServer.accept();
            start = System.currentTimeMillis();
            int in = 0;
            
            while (in != -1) {
            	current += in;
            	InputStream dIn = tcpSocket.getInputStream();
                in = dIn.read(new byte [1400]);
            }
//            dIn.close();
        } catch (IOException e) {
        }
        end = System.currentTimeMillis();
        end = (end - start) / 1000;
        dsize = current * 0.008;
        result = dsize / end;
        System.out.println("Size in byte: " + current);
        System.out.println("SIZE IN kbit: " + dsize);
        System.out.println("TIME in sec: " + end);
        System.out.println(result + " in kbit/sec");
    }

    public static void main(String[] args) {
        Server s = new Server();
        s.run();
    }

}