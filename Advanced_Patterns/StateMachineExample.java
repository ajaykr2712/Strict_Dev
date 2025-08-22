public class StateMachineExample {
    interface State { void onEnter(); State handle(String event); }
    static class Created implements State {
        @Override public void onEnter(){ System.out.println("Enter CREATED"); }
        @Override public State handle(String e){ return switch (e){ case "pay" -> new Paid(); default -> this; }; }
    }
    static class Paid implements State {
        @Override public void onEnter(){ System.out.println("Enter PAID"); }
        @Override public State handle(String e){ return switch (e){ case "ship" -> new Shipped(); case "refund" -> new Refunded(); default -> this; }; }
    }
    static class Shipped implements State {
        @Override public void onEnter(){ System.out.println("Enter SHIPPED"); }
        @Override public State handle(String e){ return this; }
    }
    static class Refunded implements State {
        @Override public void onEnter(){ System.out.println("Enter REFUNDED"); }
        @Override public State handle(String e){ return this; }
    }

    static class FSM { private State state = new Created(); void send(String event){ State next = state.handle(event); if (next != state){ state = next; state.onEnter(); } } }

    public static void main(String[] args){
        FSM fsm = new FSM();
        fsm.send("pay");
        fsm.send("ship");
    }
}
