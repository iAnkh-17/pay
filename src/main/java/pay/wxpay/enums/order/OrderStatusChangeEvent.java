package pay.wxpay.enums.order;

/**
 * @author ankh
 * @Description 订单状态变更事件枚举
 * @createTime 2023/7/27 14:11
 */
public enum OrderStatusChangeEvent {
    /**
     * 支付订单
     */
    PAY,
    /**
     * 取消订单
     */
    CANCEL,
    /**
     * 核销订单
     */
    VERIFICATION,
    /**
     * 申请退款
     */
    APPLY_REFUND,
    /**
     * 取消申请
     */
    APPLY_CANCEL,
    /**
     * 退款成功
     */
    REFUND_SUCCESS,
    /**
     * 退款失败
     */
    REFUND_FAIL;
}
