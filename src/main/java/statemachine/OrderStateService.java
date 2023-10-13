package statemachine;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.stereotype.Service;
import pay.wxpay.entiy.Order;
import pay.wxpay.enums.order.OrderStatus;
import pay.wxpay.enums.order.OrderStatusChangeEvent;

/**
 * @author ankh
 * @Description
 * @createTime 2023/8/21 16:38
 */
@Service
@Slf4j
public class OrderStateService {

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    private OrderStateMachineBuilder orderStateMachineBuilder;

    @Resource
    private StateMachinePersister<OrderStatus, OrderStatusChangeEvent, Order> nonStateMachinePersister;

    /**
     * 发送订单状态转换事件
     * 目前新版本采用flux响应式编程，暂未切换
     * StateMachine非线程安全，所以目前调用都会创建新的状态机（目前并发量并不高）。测试过2分钟内60w个并发请求，内存消耗几百兆
     * @param changeEvent 触发事件
     * @param order 订单数据
     * @param key 事件key
     * @param other 需要传递的其他数据
     * @return
     * @param <T>
     */
    @SuppressWarnings("deprecation")
    public <T> boolean sendEvent(OrderStatusChangeEvent changeEvent, Order order, String key,T other) {
        StateMachine<OrderStatus, OrderStatusChangeEvent> orderStateMachine = null;
        try {
            //创建状态机
            orderStateMachine = orderStateMachineBuilder.build(beanFactory);
            OrderMachineThreadLocal.setMachine(orderStateMachine);
        }catch (Exception e){
            log.error("创建订单状态机失败:{}",e);
        }
        try {
            //启动状态机
            orderStateMachine.start();
            //恢复状态机状态
            nonStateMachinePersister.restore(orderStateMachine, order);
            Message message = MessageBuilder.withPayload(changeEvent).setHeader(OrderAopConstants.ORDER_HEADER, order)
                    .setHeader(key,other).build();
            //发送对应的事件，监听器对其进行处理
            boolean sendEvent = orderStateMachine.sendEvent(message);
            if (!sendEvent){
                log.error("当前订单状态无法进行此操作");
                return false;
            }
            //获取到监听的结果信息
            Boolean success = (Boolean) orderStateMachine.getExtendedState().getVariables().get(key + order.getId());
            //操作完成之后,删除本次对应的key信息
            orderStateMachine.getExtendedState().getVariables().remove(key+order.getId());
            if (success == null){
                return false;
            }
            return success;
        } catch (Exception e) {
            log.error("订单操作失败:{}", e);
        } finally {
            OrderMachineThreadLocal.remove();
            orderStateMachine.stop();
            orderStateMachine = null;
        }
        return false;
    }
}
