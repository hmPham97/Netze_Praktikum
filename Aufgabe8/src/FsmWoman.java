/**
 * Finate State Machine (FSM) Java Example: Woman
 * (lecture Slides for first lecture, p. 19)
 */

/**
 * Class which models the state machine itself.
 *
 */
public class FsmWoman {
    // all states for this FSM
    enum State {
        IDLE, HI_WAIT, TIME_WAIT, NEW_MAN;
    };
    // all messages/conditions which can occur
    enum Msg {
        MEET_MAN, HI, TIME
    }
    // current state of the FSM
    private State currentState;
    // 2D array defining all transitions that can occur
    private Transition[][] transition;

    /**
     * constructor
     */
    public FsmWoman(){
        currentState = State.IDLE;
        // define all valid state transitions for our state machine
        // (undefined transitions will be ignored)
        transition = new Transition[State.values().length] [Msg.values().length];
        transition[State.IDLE.ordinal()] [Msg.MEET_MAN.ordinal()] = new SayHi();
        transition[State.HI_WAIT.ordinal()] [Msg.HI.ordinal()] = new AskForTime();
        transition[State.TIME_WAIT.ordinal()] [Msg.TIME.ordinal()] = new Finish();
        transition[State.TIME_WAIT.ordinal()] [Msg.MEET_MAN.ordinal()] = new MeetNew();
        transition[State.NEW_MAN.ordinal()] [Msg.HI.ordinal()] = new SayHi();
        System.out.println("INFO FSM constructed, current state: "+currentState);
    }

    /**
     * Process a message (a condition has occurred).
     * @param input Message or condition that has occurred.
     */
    public void processMsg(Msg input){
        System.out.println("INFO Received "+input+" in state "+currentState);
        Transition trans = transition[currentState.ordinal()][input.ordinal()];
        if(trans != null){
            currentState = trans.execute(input);
        }
        System.out.println("INFO State: "+currentState);
    }

    /**
     * Abstract base class for all transitions.
     * Derived classes need to override execute thereby defining the action
     * to be performed whenever this transition occurs.
     */
    abstract class Transition {
        abstract public State execute(Msg input);
    }

    class SayHi extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("Hi!");
            return State.HI_WAIT;
        }
    }

    class AskForTime extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("Time?");
            return State.TIME_WAIT;
        }
    }

    class Finish extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("Thank you.");
            return State.IDLE;
        }
    }

    class MeetNew extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("Bye found a new one");
            return State.NEW_MAN;
        }
    }
}
