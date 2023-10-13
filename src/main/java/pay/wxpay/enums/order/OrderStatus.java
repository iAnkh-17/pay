package pay.wxpay.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.neuron.product.paas.common.exception.NeuRonException;

/**
 * @author ankh
 * @Description
 * @createTime 2023/7/26 15:46
 */
public enum OrderStatus {
    WAITING_PAY(0,"待付款"),
    CANCEL(1,"已取消"),
    WAITING_CONSUME(2,"待核销"),
    AFTER_SALE(3,"售后中"),
    COMPLETED(4,"已完成"),
    REFUND_SUCCESS(5,"退款成功"),
    REFUND_FAILURE(6,"退款失败");

    @EnumValue
    int code;
    String desc;

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus getStatusEnum(Integer code) {
        if (code == null) {
            throw new NeuRonException("code can not be null");
        } else {
            for (OrderStatus s : OrderStatus.values()) {
                if (s.getCode() == code) {
                    return s;
                }
            }
            throw new NeuRonException("订单状态不存在");
        }
    }
}
