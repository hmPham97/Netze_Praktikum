public class main {

    public static void main(String[] args) {

        FileSender f = new FileSender("D:\\Netze_Praktikum\\Blatt_7\\sender.png", "localhost"/*"10.179.14.216"*/);
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
