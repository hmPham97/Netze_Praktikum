public class main {

    public static void main(String[] args) {
        FsmWoman f = new FsmWoman();
        f.processMsg(FsmWoman.Msg.MEET_MAN);
        f.processMsg(FsmWoman.Msg.HI);
        f.processMsg(FsmWoman.Msg.MEET_MAN);
        f.processMsg(FsmWoman.Msg.HI);

    }
}
