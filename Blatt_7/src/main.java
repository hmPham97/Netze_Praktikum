import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

public class main {

    public static void main(String[] args) {
        /*FsmWoman f = new FsmWoman();
        f.processMsg(FsmWoman.Msg.MEET_MAN);
        f.processMsg(FsmWoman.Msg.HI);
        f.processMsg(FsmWoman.Msg.MEET_MAN);
        f.processMsg(FsmWoman.Msg.HI);
        byte[] llll = new byte[4];
        for(int i = 0; i < llll.length; i++) {
            if(i == 2) {
                llll[i] = 1;
            } else llll[i] = 0;
        }
        //System.out.println(llll.length);
        for(byte b : llll) {
            System.out.println(b);
        }
        System.out.println("helloeafafaf");
        for(int i = 0; i < llll.length; i++) {
            llll[i] = (byte) (llll[i] | 3 << 5);
            System.out.println(llll[i]);
        }
        System.out.println("fhafaöfjaöfdlj");
        for (int i = 0; i < llll.length; i++) {
            llll[i] = (byte) (llll[i]  & ~ (0 << 4));
            System.out.println("shift by 4");
            System.out.println(llll[i]);
            llll[i] = (byte) (llll[i]  & ~ (0 << 6));
            System.out.println("shift by 6");
            System.out.println(llll[i]);
        }
        System.out.println("\n");
        ByteBuffer dd = ByteBuffer.allocate(4);
        int d = 127;
        dd.putInt(d);
        byte[] m = dd.array();
        for(int i = 0; i < m.length; i++) {
            System.out.println(Integer.toBinaryString(m[i]));
        }
        System.out.println(1 << 2);
        //System.out.println(llll[1]);

        //System.out.println((byte) (2 & ~ (1 << 6)));
*/
        FileSender f = new FileSender("D:\\Netze_Praktikum\\Blatt_7\\receiver.PNg", "localhost");

        /*try {
            byte[] f = new byte[]{1,100,124,127};
            DatagramPacket p = new DatagramPacket(f,f.length);
            for(int i = 0; i < f.length; i++) {
                System.out.println(f[i]);
                System.out.println(p.getData()[i]);
            }
            ByteBuffer g = ByteBuffer.allocate(4);
            g.putInt(100);
            byte[] d = g.array();
            DatagramPacket m = new DatagramPacket(d, d.length);
            for(int i = 0; i < d.length; i++) {
                System.out.println(d[i] + "    " + m.getData()[i]);
            }
            throw new IOException();
        } catch (IOException e) {
            System.err.println("lopafajfd");
        } */
    }
}
