import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Time;
import java.util.Timer;

public class Client {
    private static String address = "localhost";
    private static int port = 7999;
    private static long time = 0;
    private static int endTime = 10;
    private static double sendRate = 0;


    private static String Paul = "10.179.0.214";
    private static boolean UDP = false;



    //private int delay = Integer.parseInt(args[0]);
    private static int length = 0;
    private static long start;
    //private int packetSent = Integer.parseInt(args[1]);
    private static int sent = 0;
    private static int sendToserver = 0;
    static byte[] buf;

    public static void main(String[] args) {
        int delay = 1;
        int packetSent = 1;
        if (UDP) {
            udpClient(delay, packetSent);
        } else {
            tcpClient(delay, packetSent);
        }
        sendRate = (length * 0.008) / time;
        System.out.println("packets sent:" + sendToserver);
        System.out.println("LENGHT = " + length);
        System.out.println("Length in kbit: " + length * 0.008);
        System.out.println("TIME USED: " + time);
        System.out.println(sendRate + " in kbit/s");
    }

    public static void udpClient(int delay, int offset) {
        start = System.currentTimeMillis();
        try {
            while (time < endTime) {
                DatagramSocket socket = new DatagramSocket();
                buf = new byte[1400];
                InetAddress adr = InetAddress.getByName(address);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, adr, port);
                length = length + buf.length;
                socket.send(packet);
                sent++;
                sendToserver++;
                if (sent == offset) {
                    sent = 0;
                    Thread.sleep(delay);
                }
                time = (System.currentTimeMillis() - start) / 1000;

                socket.close();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void tcpClient(int delay, int offset) {
        start = System.currentTimeMillis();
        try {
            InetAddress adr = InetAddress.getByName(address);
            Socket tcpSocket = new Socket(adr, port);
            DataOutputStream dOut = new DataOutputStream(tcpSocket.getOutputStream());
            while (time < endTime) {
                buf = new byte[1400];
                dOut.write(buf);
                dOut.flush();
                length = length + buf.length;
                sent++;
                sendToserver++;
                if (sent == offset) {
                    sent = 0;
                    Thread.sleep(delay);
                }
                time = (System.currentTimeMillis() - start) / 1000;
            }
            tcpSocket.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
