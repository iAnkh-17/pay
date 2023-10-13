package pay.wxpay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import pay.wxpay.entiy.Order;
import pay.wxpay.enums.wxpay.WechatPayEnum;
import pay.wxpay.enums.wxpay.WechatPayUrl;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.util.*;

/**
 * @author ankh
 * @Description 微信支付实现：v3版本
 *              https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_5_1.shtml
 * @createTime 2023/8/10 17:07
 */

@Slf4j
@Service
public class WeChatPayService {

    @Resource
    private WechatPayConfig wechatPayConfig;

    @Resource
    private WechatPayRequest wechatPayRequest;

    @Resource
    private CloseableHttpClient wxPayNoSignClient;

    @Resource
    private Verifier verifier;

    /**
     * 支付订单
     * @param type WechatPayEnum
     * @param order 对应系统订单
     * @param payMoney 支付金额，单位为分
     * @param openId 支付openId
     * @return
     */
    public Map<String,Object> pay(WechatPayEnum type, Order order, BigDecimal payMoney, String openId){
        Assert.isTrue(type== WechatPayEnum.SUB_JSAPI,"目前仅支持微信小程序支付");
        // 统一参数封装
        Map<String, Object> params = new HashMap<>(8);
        params.put("appid", wechatPayConfig.getAppId());
        params.put("mchid", wechatPayConfig.getMchId());
        params.put("description", "商品："+order.getProductName());
        params.put("out_trade_no", order.getOrderNo());
        params.put("notify_url", wechatPayConfig.getNotifyUrl());
        //支付金额
        Map<String, Object> amountMap = new HashMap<>(4);
        // 金额单位为分
        amountMap.put("total", payMoney.multiply(new BigDecimal("100")).longValue());
        amountMap.put("currency", "CNY");
        params.put("amount", amountMap);
        //支付者
        Map<String, Object> payerMap = new HashMap<>(4);
        payerMap.put("openid", openId);
        params.put("payer", payerMap);

        String paramsStr = JSON.toJSONString(params);
        log.info("请求参数 ===> {}" + paramsStr);

        String prefix = type.getType();

        if (type==WechatPayEnum.SUB_JSAPI){
            //小程序支付和jsapi支付相同
            //重写type值，因为小程序会多一个下划线(sub_type)
            String[] split = type.getType().split("_");
            prefix = split[split.length - 1];
        }

        String resStr = wechatPayRequest.wechatHttpPost(wechatPayConfig.getBaseUrl().concat(WechatPayUrl.PAY_V3.getSuffix().concat(prefix)), paramsStr);

        Map<String, Object> resMap = JSONObject.parseObject(resStr, new TypeReference<Map<String, Object>>(){});
        Map<String, Object> signMap = paySignMsg(resMap, type);
        resMap.put("type",type);
        resMap.put("signMap",signMap);
        return resMap;
    }

    /**
     * 查询订单状态
     *
     * 需要调用查询接口的情况：
     *
     * • 当商户后台、网络、服务器等出现异常，商户系统最终未接收到支付通知。
     *
     * • 调用支付接口后，返回系统错误或未知交易状态情况。
     *
     * • 调用付款码支付API，返回USERPAYING的状态。
     *
     * • 调用关单或撤销接口API之前，需确认支付状态。
     * @param orderNo
     * @return
     */
    public String query(String orderNo){
        log.info("根据订单号查询订单，订单号： {}", orderNo);

        String url = wechatPayConfig.getBaseUrl().concat(WechatPayUrl.ORDER_QUERY_BY_NO.getSuffix().concat(orderNo))
                .concat("?mchid=").concat(wechatPayConfig.getMchId());

        String res = wechatPayRequest.wechatHttpGet(url);

        Map<String, Object> resMap = JSONObject.parseObject(res, new TypeReference<Map<String, Object>>(){});

        String tradeState = resMap.get("trade_state").toString();

        /**
         *  交易状态，枚举值：
         *  SUCCESS：支付成功
         *  REFUND：转入退款
         *  NOTPAY：未支付
         *  CLOSED：已关闭
         *  REVOKED：已撤销（仅付款码支付会返回）
         *  USERPAYING：用户支付中（仅付款码支付会返回）
         *  PAYERROR：支付失败（仅付款码支付会返回）
         */
        return tradeState;
    }

    /**
     * 客户下单后，不进行支付，取消微信订单
     * @param orderNo
     */
    public void closeOrder(String orderNo){

        log.info("根据订单号取消订单，订单号： {}", orderNo);

        String url = String.format(WechatPayUrl.CLOSE_ORDER_BY_NO.getSuffix(), orderNo);
        url = wechatPayConfig.getBaseUrl().concat(url);

        // 设置参数
        Map<String, String> params = new HashMap<>(2);
        params.put("mchid", wechatPayConfig.getMchId());

        String paramsStr = JSON.toJSONString(params);

        wechatPayRequest.wechatHttpPost(url,paramsStr);
    }

    /**
     * 微信退款
     * @param order 订单
     * @param total 退款金额
     * @return
     */
    public String refundOrder(Order order,BigDecimal total){
        log.info("根据订单号申请退款，订单号： {}", order.getOrderNo());
        String url = wechatPayConfig.getBaseUrl().concat(WechatPayUrl.APPLY_REFUNDS.getSuffix());
        // 设置参数
        Map<String, Object> params = new HashMap<>(2);
        // 订单编号
        params.put("out_trade_no", order.getOrderNo());
        // 退款编号
        params.put("out_refund_no",order.getRefundNo());
        // 退款原因
        params.put("reason","退款原因");
        // 退款通知回调地址
        params.put("notify_url", wechatPayConfig.getRefundNotifyUrl());

        Map<String, Object>  amountMap =new HashMap<>();
        //退款金额，单位：分
        long l = total.multiply(new BigDecimal("100")).longValue();
        amountMap.put("refund", l);
        //原订单金额，单位：分
        amountMap.put("total", l);
        //退款币种
        amountMap.put("currency", "CNY");
        params.put("amount", amountMap);

        String paramsStr = JSON.toJSONString(params);
        log.info("请求参数 ===> {}" + paramsStr);


        String res = wechatPayRequest.wechatHttpPost(url,paramsStr);

        Map<String, Object> resMap = JSONObject.parseObject(res, new TypeReference<Map<String, Object>>(){});
        /**
         * 枚举值：
         * SUCCESS：退款成功
         * CLOSED：退款关闭
         * PROCESSING：退款处理中
         * ABNORMAL：退款异常
         */
        String status = resMap.get("status").toString();
        return status;
    }

    /**
     * 查询单笔退款信息
     * 提交退款申请后，通过调用该接口查询退款状态。退款有一定延时，建议在提交退款申请后1分钟发起查询退款状态，一般来说零钱支付的退款5分钟内到账，银行卡支付的退款1-3个工作日到账。
     * @param refundNo
     * @return
     */
    public Map<String, Object> queryRefundOrder(String refundNo) {

        log.info("根据退款号查询退款订单，退款号： {}", refundNo);

        String url = wechatPayConfig.getBaseUrl().concat(WechatPayUrl.REFUNDS_QUERY.getSuffix().concat(refundNo));

        String res = wechatPayRequest.wechatHttpGet(url);

        Map<String, Object> resMap = JSONObject.parseObject(res, new TypeReference<Map<String, Object>>(){});

        String successTime = resMap.get("success_time").toString();
        String refundId = resMap.get("refund_id").toString();
        /**
         * 款到银行发现用户的卡作废或者冻结了，导致原路退款银行卡失败，可前往商户平台-交易中心，手动处理此笔退款。
         * 枚举值：
         * SUCCESS：退款成功
         * CLOSED：退款关闭
         * PROCESSING：退款处理中
         * ABNORMAL：退款异常
         */
        String status = resMap.get("status").toString();

        /**
         * 枚举值：
         * ORIGINAL：原路退款
         * BALANCE：退回到余额
         * OTHER_BALANCE：原账户异常退到其他余额账户
         * OTHER_BANKCARD：原银行卡异常退到其他银行卡
         */
        String channel = resMap.get("channel").toString();

        log.info("successTime："+successTime);
        log.info("channel："+channel);
        log.info("refundId："+refundId);
        log.info("status："+status);

        return resMap;
    }

    /**
     * 支付回调
     * @param request
     * @param response
     * @return
     */
    public Map<String, String> payNotify(HttpServletRequest request, HttpServletResponse response) {
        // 处理通知参数
        Map<String,Object> bodyMap = getNotifyBody(request);
        if(bodyMap==null){
            return falseMsg(response);
        }
        // 解密resource中的通知数据
        String resource = bodyMap.get("resource").toString();
        Map<String, Object> resourceMap = WechatPayValidator.decryptFromResource(resource, wechatPayConfig.getApiV3Key(),1);
        //订单号
        String orderNo = resourceMap.get("out_trade_no").toString();
        //支付状态
        String tradeState = resourceMap.get("trade_state").toString();
        //微信流水号
        String transactionId = resourceMap.get("transaction_id").toString();

        //=========== 在对业务数据进行状态检查和处理之前，要采用锁进行并发控制，以避免微信重试策略造成的数据异常（幂等性） ===========
        //lock.lock()
        //业务
        //lock.unlock()
        //成功应答
        return trueMsg(response);
    }

    /**
     * 退款回调
     * @param request
     * @param response
     * @return
     */
    public Map<String, String> refundNotify(HttpServletRequest request, HttpServletResponse response) {
        log.info("退款回调");
        // 处理通知参数
        Map<String,Object> bodyMap = getNotifyBody(request);
        if(bodyMap==null){
            return falseMsg(response);
        }
        // 解密resource中的通知数据
        String resource = bodyMap.get("resource").toString();
        Map<String, Object> resourceMap = WechatPayValidator.decryptFromResource(resource, wechatPayConfig.getApiV3Key(),2);
        //订单号
        String orderNo = resourceMap.get("out_trade_no").toString();
        //订单退款编号
        String refundNo = resourceMap.get("out_refund_no").toString();
        //微信退款编号
        String refundId = resourceMap.get("refund_id").toString();
        //退款状态
        String refundStatus = resourceMap.get("refund_status").toString();

        //=========== 在对业务数据进行状态检查和处理之前，要采用锁进行并发控制，以避免微信重试策略造成的数据异常（幂等性） ===========
        //lock.lock()
        //业务
        //lock.unlock()
        //成功应答
        return trueMsg(response);
    }

    private Map<String, Object> paySignMsg(Map<String, Object> map,WechatPayEnum type){
        // 设置签名信息,Native与H5不需要
        if((type==WechatPayEnum.H5) || type.equals(WechatPayEnum.NATIVE)){
            return null;
        }

        long timeMillis = System.currentTimeMillis();
        String appId = wechatPayConfig.getAppId();
        String timeStamp = timeMillis/1000+"";
        String nonceStr = timeMillis+"";
        String prepayId = map.get("prepay_id").toString();
        String packageStr = "prepay_id="+prepayId;
        // 公共参数
        Map<String, Object> resMap = new HashMap<>();
        resMap.put("nonceStr",nonceStr);
        resMap.put("timeStamp",timeStamp);
        // JSAPI、SUB_JSAPI(小程序)
        resMap.put("appId",appId);
        resMap.put("package", packageStr);
        // 使用字段appId、timeStamp、nonceStr、package进行签名
        String paySign = null;
        try {
            paySign = getSign(appId,timeMillis / 1000,nonceStr,packageStr);
        } catch (Exception e) {
            log.error("构造签名失败",e);
        }
        resMap.put("paySign", paySign);
        resMap.put("signType", "RSA");
        return resMap;
    }

    /**
     * 获取加密数据
     */
    private String createSign(Map<String, Object> params){
        try {
            Map<String, Object> treeMap = new TreeMap<>(params);
            List<String> signList = new ArrayList<>(5);
            for (Map.Entry<String, Object> entry : treeMap.entrySet()) {
                signList.add(entry.getKey() + "=" + entry.getValue());
            }
            String signStr = String.join("&", signList);

            signStr = signStr+"&key="+wechatPayConfig.getApiV3Key();

            Mac sha = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(wechatPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha.init(secretKey);
            byte[] array = sha.doFinal(signStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
            }
            signStr = sb.toString().toUpperCase();
            log.info(signStr);
            return signStr;
        }catch (Exception e){
            throw new RuntimeException("加密失败！");
        }
    }

    /**
     * 作用：使用字段appId、timeStamp、nonceStr、package计算得出的签名值
     * 场景：根据微信统一下单接口返回的 prepay_id 生成调启支付所需的签名值
     * @param appId
     * @param timestamp
     * @param nonceStr
     * @param pack package
     * @return
     * @throws Exception
     */
    public String getSign(String appId, long timestamp, String nonceStr, String pack) throws Exception{
        String message = buildMessage(appId, timestamp, nonceStr, pack);
        String paySign= sign(message.getBytes("utf-8"));
        return paySign;
    }

    private String buildMessage(String appId, long timestamp, String nonceStr, String pack) {
        return appId + "\n"
                + timestamp + "\n"
                + nonceStr + "\n"
                + pack + "\n";
    }
    private String sign(byte[] message) throws Exception{
        Signature sign = Signature.getInstance("SHA256withRSA");
        //这里需要一个PrivateKey类型的参数，就是商户的私钥。
        sign.initSign(wechatPayConfig.doGetPrivateKey());
        sign.update(message);
        return Base64.getEncoder().encodeToString(sign.sign());
    }

    private Map<String,Object> getNotifyBody(HttpServletRequest request){
        //处理通知参数
        String body = HttpUtil.ReadAsStr(request);
        log.info("回调参数：{}",body);
        // 转换为Map
        Map<String, Object> bodyMap = JSONObject.parseObject(body, new TypeReference<Map<String, Object>>(){});
        // 微信的通知ID（通知的唯一ID）
        String notifyId = bodyMap.get("id").toString();

        // 验证签名信息
        WechatPayValidator wechatPayValidator
                = new WechatPayValidator(verifier, notifyId, body);
        if(!wechatPayValidator.validate(request)){
            log.error("通知验签失败");
            return null;
        }
        log.info("通知验签成功");
        return bodyMap;
    }

    private Map<String, String> falseMsg(HttpServletResponse response){
        Map<String, String> resMap = new HashMap<>(8);
        //失败应答
        response.setStatus(500);
        resMap.put("code", "FAIL");
        resMap.put("message", "失败");
        return resMap;
    }

    private Map<String, String> trueMsg(HttpServletResponse response){
        Map<String, String> resMap = new HashMap<>(8);
        //成功应答
        response.setStatus(200);
        resMap.put("code", "SUCCESS");
        resMap.put("message", "成功");
        return resMap;
    }
}
