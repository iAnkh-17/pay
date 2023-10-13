package pay.wxpay.entiy;

import lombok.Data;
import pay.wxpay.enums.order.OrderStatus;

/**
 * @author ankh
 * @Description
 * @createTime 2023/10/13 17:13
 */
@Data
public class Order {

    String id;

    String orderNo;

    String productName;

    String refundNo;

    OrderStatus orderStatus;
}
