public class main2 {
    public static void main(String[] args) {
        FileReceiver f = new FileReceiver();
        String filename;
        f.start();
        FileReceiver.Reply reply;
        f.start();
        boolean goThru = true;
        while (goThru) {
            if(f.getCurrentState() == FileReceiver.State.WaitingForName) {
                filename = f.getFileName();
                if(f.getCurrentState() == FileReceiver.State.ReceivedFromServerName) {
                    f.startPath(filename);
                    reply = f.getNameReply();
                    f.sendReply(reply);
                }
            }
            //else if (f.getCurrentState() == FileReceiver.State.ReceivedFromServerName) {
            //}
            else if (f.getCurrentState() == FileReceiver.State.WaitForPacket) {
                f.waitingForPacket();
            }
            else if (f.getCurrentState() == FileReceiver.State.ReceivedFromServer) {
                reply = f.getReply();
                f.sendReply(reply);
            } else if(f.getCurrentState() == FileReceiver.State.Done) {
                goThru = false;
            }
        }

    }
}
