package statemachine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import pay.wxpay.enums.order.OrderStatus;
import pay.wxpay.enums.order.OrderStatusChangeEvent;

import java.util.EnumSet;

/**
 * @author ankh
 * @Description 订单状态机配置
 * @createTime 2023/7/27 17:47
 */
@Configuration
@EnableStateMachine(name = "orderStateMachine")
@Slf4j
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderStatus, OrderStatusChangeEvent> {
    /**
     * 配置状态
     *
     * @param states
     * @throws Exception
     */
    public void configure(StateMachineStateConfigurer<OrderStatus, OrderStatusChangeEvent> states) throws Exception {
        states
                .withStates()
                .initial(OrderStatus.WAITING_PAY)
                .states(EnumSet.allOf(OrderStatus.class));
    }
    /**
     * 配置状态转换事件关系
     *
     * @param transitions
     * @throws Exception
     */
    public void configure(StateMachineTransitionConfigurer<OrderStatus, OrderStatusChangeEvent> transitions) throws Exception {
        transitions
                //支付事件:待支付-》待核销
                .withExternal().source(OrderStatus.WAITING_PAY).target(OrderStatus.WAITING_CONSUME).event(OrderStatusChangeEvent.PAY)
                .and()
                //取消事件:待支付-》已取消
                .withExternal().source(OrderStatus.WAITING_PAY).target(OrderStatus.CANCEL).event(OrderStatusChangeEvent.CANCEL)
                .and()
                //核销订单事件:待核销-》已完成
                .withExternal().source(OrderStatus.WAITING_CONSUME).target(OrderStatus.COMPLETED).event(OrderStatusChangeEvent.VERIFICATION)
                .and()
                //申请退款事件:待核销-》售后中
                .withExternal().source(OrderStatus.WAITING_CONSUME).target(OrderStatus.AFTER_SALE).event(OrderStatusChangeEvent.APPLY_REFUND)
                .and()
                //取消申请事件:售后中-》待核销
                .withExternal().source(OrderStatus.AFTER_SALE).target(OrderStatus.WAITING_CONSUME).event(OrderStatusChangeEvent.APPLY_CANCEL)
                .and()
                //退款成功事件:售后中-》退款成功
                .withExternal().source(OrderStatus.AFTER_SALE).target(OrderStatus.REFUND_SUCCESS).event(OrderStatusChangeEvent.REFUND_SUCCESS)
                .and()
                //退款失败事件:售后中-》退款失败
                .withExternal().source(OrderStatus.AFTER_SALE).target(OrderStatus.REFUND_FAILURE).event(OrderStatusChangeEvent.REFUND_FAIL);
    }

}
