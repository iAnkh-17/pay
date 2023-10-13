package pay.wxpay.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ankh
 * @Description
 * @createTime 2023/8/11 16:08
 */
@AllArgsConstructor
@Getter
public enum WechatPayUrl {

    /**
     * 微信下单
     */
    PAY_V3("/pay/transactions/"),
    /**
     * 查询订单
     */
    ORDER_QUERY_BY_NO("/pay/transactions/out-trade-no/"),
    /**
     * 关闭订单
     */
    CLOSE_ORDER_BY_NO("/pay/transactions/out-trade-no/%s/close"),
    /**
     * 申请退款
     */
    APPLY_REFUNDS("/refund/domestic/refunds"),
    /**
     * 查询单笔退款
     */
    REFUNDS_QUERY("/refund/domestic/refunds/");

    private String suffix;
}
