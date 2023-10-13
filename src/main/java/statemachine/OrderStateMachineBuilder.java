package statemachine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import pay.wxpay.enums.order.OrderStatus;
import pay.wxpay.enums.order.OrderStatusChangeEvent;

import java.util.EnumSet;

/**
 * @author ankh
 * @Description
 * @createTime 2023/7/28 14:35
 */
@Configuration
@Slf4j
public class OrderStateMachineBuilder {

    private final static String MACHINEID = "orderStateMachine";

    /**
     * 构建状态机
     *
     * @param beanFactory
     * @return
     * @throws Exception
     */
    public StateMachine<OrderStatus, OrderStatusChangeEvent> build(BeanFactory beanFactory) throws Exception {
        StateMachineBuilder.Builder<OrderStatus, OrderStatusChangeEvent> builder = StateMachineBuilder.builder();
        builder.configureConfiguration()
                .withConfiguration()
                .machineId(MACHINEID)
                .beanFactory(beanFactory);

        builder.configureStates()
                .withStates()
                .initial(OrderStatus.WAITING_PAY)
                .states(EnumSet.allOf(OrderStatus.class));

        builder.configureTransitions()
                .withExternal()
                //支付事件:待支付-》待核销
                .source(OrderStatus.WAITING_PAY).target(OrderStatus.WAITING_CONSUME).event(OrderStatusChangeEvent.PAY)
                .and()
                .withExternal()
                //取消事件:待支付-》已取消
                .source(OrderStatus.WAITING_PAY).target(OrderStatus.CANCEL).event(OrderStatusChangeEvent.CANCEL)
                .and()
                //核销订单事件:待核销-》已完成
                .withExternal()
                .source(OrderStatus.WAITING_CONSUME).target(OrderStatus.COMPLETED).event(OrderStatusChangeEvent.VERIFICATION)
                .and()
                //申请退款事件:待核销-》售后中
                .withExternal()
                .source(OrderStatus.WAITING_CONSUME).target(OrderStatus.AFTER_SALE).event(OrderStatusChangeEvent.APPLY_REFUND)
                .and()
                //取消申请事件:售后中-》待核销
                .withExternal()
                .source(OrderStatus.AFTER_SALE).target(OrderStatus.WAITING_CONSUME).event(OrderStatusChangeEvent.APPLY_CANCEL)
                .and()
                //退款成功事件:售后中-》退款成功
                .withExternal()
                .source(OrderStatus.AFTER_SALE).target(OrderStatus.REFUND_SUCCESS).event(OrderStatusChangeEvent.REFUND_SUCCESS)
                .and()
                //退款失败事件:售后中-》退款成功
                .withExternal()
                .source(OrderStatus.AFTER_SALE).target(OrderStatus.REFUND_FAILURE).event(OrderStatusChangeEvent.REFUND_FAIL);

        return builder.build();
    }

    /**
     * （1）Action 可以用来在状态机的状态转换过程中实现自定义逻辑，如数据库操作，日志记录等。
     * （2）注意，如果 Action 执行过程中出现了异常，状态机的状态是不会发生变化的。
     *
     * @return
     */
    @Bean
    public Action<OrderStatus, OrderStatusChangeEvent> action() {
        return context -> {

        };
    }
}
