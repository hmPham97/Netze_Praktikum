public class main {

    public static void main(String[] args) {
        String one = "10.179.14.216";
        String lukas = "10.179.12.46";
        String two = "localhost";
        String testrar = "D:\\Netze_Praktikum\\Blatt_7\\test.rar";
        String testpic = "D:\\Netze_Praktikum\\Blatt_7\\sender.png";
        FileSender f = new FileSender(testpic, lukas);
        f.start();
        boolean goThru = true;
        int off;
        while(goThru) {
            off = f.getOffset();
            if(f.getCurrentState() == FileSender.State.SendingName) {
                f.sendFileName(f.getNameOfFile().getBytes());
                f.send(0);
            }
            else if(f.getCurrentState() == FileSender.State.WaitingForACKNamePacket) {
                f.waitForACK();
                if(f.getCurrentState() == FileSender.State.WaitingForACKNamePacket) {
                    f.send(0);
                }
            }
            else if(f.getCurrentState() == FileSender.State.SendPacket) {
                f.send(off);
            }
            else if(f.getCurrentState() == FileSender.State.WaitingForACK) {
                f.waitForACK();
                if(f.getCurrentState() == FileSender.State.WaitingForACK) {
                    f.send(off);
                }
            }
            else if(f.getCurrentState() == FileSender.State.Done) {
                goThru = false;
            }
        }

    }
}
