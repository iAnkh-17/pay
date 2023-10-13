package statemachine;


import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.OnTransition;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import pay.wxpay.entiy.Order;
import pay.wxpay.enums.order.OrderStatus;
import pay.wxpay.enums.order.OrderStatusChangeEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author ankh
 * @Description 订单事件监听器
 * @createTime 2023/7/28 13:48
 */
@Component("orderStateListener")
@WithStateMachine(name = "orderStateMachine")
@Slf4j
public class OrderStateListener {


    @OnTransition(source = "WAITING_PAY", target = "WAITING_CONSUME")
    @Transactional(rollbackFor = Exception.class)
    public void payTransition(Message<OrderStatusChangeEvent> message) {
        String orderId = "";
        //使用AOP没办法回滚，所以在业务里设置对应的执行状态
        StateMachine stateMachine = OrderMachineThreadLocal.getMachine();
        try {
            Order order = (Order) message.getHeaders().get(OrderAopConstants.ORDER_HEADER);
            log.info("支付订单，状态机反馈信息：{}", message.getHeaders());
            //执行成功
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.PAY_TRANSITION + orderId, OrderAopConstants.SUCCESS_FLAG);
        }catch (Exception e){
            //执行异常
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.PAY_TRANSITION + orderId, OrderAopConstants.FAILED_FLAG);
            throw e;
        }
    }

    @OnTransition(source = "WAITING_PAY", target = "CANCEL")
    @Transactional(rollbackFor = Exception.class)
    public void cancleTransition(Message<OrderStatusChangeEvent> message) {
        String orderId = "";
        StateMachine stateMachine = OrderMachineThreadLocal.getMachine();
        try {
            Order order = (Order) message.getHeaders().get(OrderAopConstants.ORDER_HEADER);
            log.info("取消订单，状态机反馈信息：{}", message.getHeaders());
            //执行成功
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.CANCEL_TRANSITION + orderId, OrderAopConstants.SUCCESS_FLAG);
        }catch (Exception e){
            //执行异常
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.CANCEL_TRANSITION + orderId, OrderAopConstants.FAILED_FLAG);
            throw e;
        }
    }

    @OnTransition(source = "WAITING_CONSUME", target = "AFTER_SALE")
    @Transactional(rollbackFor = Exception.class)
    public void afterSaleTransition(Message<OrderStatusChangeEvent> message) {
        String orderId = "";
        StateMachine stateMachine = OrderMachineThreadLocal.getMachine();
        try {
            Order order = (Order) message.getHeaders().get(OrderAopConstants.ORDER_HEADER);
            log.info("申请售后，状态机反馈信息：{}", message.getHeaders());
            //执行成功
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.APPLY_REFUND_TRANSITION + orderId, OrderAopConstants.SUCCESS_FLAG);
        }catch (Exception e){
            //执行异常
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.APPLY_REFUND_TRANSITION + orderId, OrderAopConstants.FAILED_FLAG);
            throw e;
        }
    }

    @OnTransition(source = "AFTER_SALE", target = "WAITING_CONSUME")
    @Transactional(rollbackFor = Exception.class)
    public void cancelAfterSaleTransition(Message<OrderStatusChangeEvent> message) {
        String orderId = "";
        StateMachine stateMachine = OrderMachineThreadLocal.getMachine();
        try {
            Order order = (Order) message.getHeaders().get(OrderAopConstants.ORDER_HEADER);
            log.info("取消售后，状态机反馈信息：{}", message.getHeaders());
            //执行成功
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.CANCEL_REFUND_TRANSITION + orderId, OrderAopConstants.SUCCESS_FLAG);
        }catch (Exception e){
            //执行异常
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.CANCEL_REFUND_TRANSITION + orderId, OrderAopConstants.FAILED_FLAG);
            throw e;
        }
    }

    @OnTransition(source = "AFTER_SALE", target = "REFUND_SUCCESS")
    @Transactional(rollbackFor = Exception.class)
    public void passAfterSaleTransition(Message<OrderStatusChangeEvent> message) {
        String orderId = "";
        StateMachine stateMachine = OrderMachineThreadLocal.getMachine();
        try {
            Order order = (Order) message.getHeaders().get(OrderAopConstants.ORDER_HEADER);
            log.info("退款成功，状态机反馈信息：{}", message.getHeaders());
            //执行成功
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.PASS_REFUND_TRANSITION + orderId, OrderAopConstants.SUCCESS_FLAG);
        }catch (Exception e){
            //执行异常
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.PASS_REFUND_TRANSITION + orderId, OrderAopConstants.FAILED_FLAG);
            throw e;
        }
    }

    @OnTransition(source = "AFTER_SALE", target = "REFUND_FAILURE")
    @Transactional(rollbackFor = Exception.class)
    public void refuseAfterSaleTransition(Message<OrderStatusChangeEvent> message) {
        String orderId = "";
        StateMachine stateMachine = OrderMachineThreadLocal.getMachine();
        try {
            Order order = (Order) message.getHeaders().get(OrderAopConstants.ORDER_HEADER);
            log.info("退款失败，状态机反馈信息：{}", message.getHeaders());
            //执行成功
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.REFUSE_REFUND_TRANSITION + orderId, OrderAopConstants.SUCCESS_FLAG);
        }catch (Exception e){
            //执行异常
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.REFUSE_REFUND_TRANSITION + orderId, OrderAopConstants.FAILED_FLAG);
            throw e;
        }
    }

    @OnTransition(source = "WAITING_CONSUME", target = "COMPLETED")
    @Transactional(rollbackFor = Exception.class)
    public void verificationTransition(Message<OrderStatusChangeEvent> message) {
        String orderId = "";
        StateMachine stateMachine = OrderMachineThreadLocal.getMachine();
        try {
            Order order = (Order) message.getHeaders().get(OrderAopConstants.ORDER_HEADER);
            log.info("核销订单，状态机反馈信息：{}", message.getHeaders());
            //执行成功
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.VERIFICATION_TRANSITION + orderId, OrderAopConstants.SUCCESS_FLAG);
        }catch (Exception e){
            //执行异常
            stateMachine.getExtendedState().getVariables().put(OrderAopConstants.VERIFICATION_TRANSITION + orderId, OrderAopConstants.FAILED_FLAG);
            throw e;
        }
    }
}
