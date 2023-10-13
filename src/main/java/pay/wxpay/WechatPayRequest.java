package pay.wxpay;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ankh
 * @Description
 * @createTime 2023/8/11 15:32
 */

@Slf4j
@Component
public class WechatPayRequest {

    @Resource
    private CloseableHttpClient wxPayClient;

    public String wechatHttpGet(String url) {
        try {
            // 拼接请求参数
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");

            //完成签名并执行请求
            CloseableHttpResponse response = wxPayClient.execute(httpGet);

            return getResponseBody(response);
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

    public String wechatHttpPost(String url,String paramsStr) {
        try {
            HttpPost httpPost = new HttpPost(url);
            StringEntity entity = new StringEntity(paramsStr, "utf-8");
            entity.setContentType("application/json");
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");

            CloseableHttpResponse response = wxPayClient.execute(httpPost);
            return getResponseBody(response);
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

    private String getResponseBody(CloseableHttpResponse response) throws IOException {

        //响应体
        HttpEntity entity = response.getEntity();
        String body = entity==null?"": EntityUtils.toString(entity);
        //响应状态码
        int statusCode = response.getStatusLine().getStatusCode();

        //处理成功,204是，关闭订单时微信返回的正常状态码
        if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_NO_CONTENT) {
            log.info("成功, 返回结果 = " + body);
        } else {
            String msg = "微信支付请求失败,响应码 = " + statusCode + ",返回结果 = " + body;
            log.error(msg);
            throw new RuntimeException(msg);
        }
        return body;
    }
}
