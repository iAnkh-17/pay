package statemachine;

import org.springframework.statemachine.StateMachine;

/**
 * @author ankh
 * @Description
 * @createTime 2023/7/31 10:02
 */
public final class OrderMachineThreadLocal {

    private static final ThreadLocal<StateMachine> ORDER_STATE_MACHINE = new ThreadLocal<>();

    public static void setMachine(StateMachine stateMachine) {
        ORDER_STATE_MACHINE.set(stateMachine);
    }

    public static StateMachine getMachine() {
        StateMachine stateMachine = ORDER_STATE_MACHINE.get();
        return stateMachine;
    }

    public static void remove() {
        StateMachine stateMachine = ORDER_STATE_MACHINE.get();
        if (stateMachine!=null){
            ORDER_STATE_MACHINE.remove();
        }
    }
}
