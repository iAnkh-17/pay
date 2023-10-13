package statemachine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import pay.wxpay.entiy.Order;
import pay.wxpay.enums.order.OrderStatus;
import pay.wxpay.enums.order.OrderStatusChangeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ankh
 * @Description 状态机持久化配置
 *              1.map
 *              2.redis
 *              3.伪持久化(采用)
 *              真实业务场景需要从数据库读取订单的状态，暂不考虑持久化操作
 * @createTime 2023/7/28 10:55
 */
@Configuration
@Slf4j
public class Persist<E, S> {

    /**
     * 持久化到内存map中
     *
     * @return
     */
    @Bean(name = "stateMachineMemPersister")
    public static StateMachinePersister getPersister() {
        return new DefaultStateMachinePersister(new StateMachinePersist() {
            private Map map = new ConcurrentHashMap();
            @Override
            public void write(StateMachineContext context, Object contextObj) {
                map.put(contextObj, context);
            }
            @Override
            public StateMachineContext read(Object contextObj) {
                StateMachineContext stateMachineContext = (StateMachineContext) map.get(contextObj);
                return stateMachineContext;
            }
        });
    }

    /**
     * 伪持久化
     */
    @Bean(name = "nonStateMachinePersister")
    public static StateMachinePersister noPersister() {
        return new DefaultStateMachinePersister(new StateMachinePersist<OrderStatus, OrderStatusChangeEvent, Order>() {
            @Override
            public void write(StateMachineContext<OrderStatus, OrderStatusChangeEvent> context, Order contextObj) {
                //未做任何持久化处理
                //原因是订单的状态可以直接从数据库读取
            }

            @Override
            public StateMachineContext<OrderStatus, OrderStatusChangeEvent> read(Order contextObj) {
                StateMachineContext<OrderStatus, OrderStatusChangeEvent> result = new DefaultStateMachineContext<>(contextObj.getOrderStatus(),
                        null, null, null, null, "orderStateMachine");
                return result;
            }
        });
    }
}
