package statemachine;

/**
 * @author ankh
 * @Description
 * @createTime 2023/7/28 17:44
 */
public interface OrderAopConstants {

   String ORDER_HEADER = "order";

   String PAY_TRANSITION = "payTransition";

   String CANCEL_TRANSITION = "cancelTransition";

   String APPLY_REFUND_TRANSITION = "applyRefundTransition";

   String CANCEL_REFUND_TRANSITION = "cancelRefundTransition";

   String PASS_REFUND_TRANSITION = "passRefundTransition";

   String REFUSE_REFUND_TRANSITION = "refuseRefundTransition";

   String VERIFICATION_TRANSITION = "verificationTransition";

   Boolean SUCCESS_FLAG = Boolean.TRUE;

   Boolean FAILED_FLAG = Boolean.FALSE;
}
