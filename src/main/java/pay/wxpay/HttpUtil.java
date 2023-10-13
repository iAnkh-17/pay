package pay.wxpay;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author ankh
 * @Description
 * @createTime 2023/8/23 17:33
 */
@Slf4j
public class HttpUtil {


    // 字符串读取
    public static String ReadAsStr(HttpServletRequest request) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder("");
        try {
            br = request.getReader();
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            br.close();
        }
        catch (IOException e) {
            log.error("解析http请求数据失败",e);
        }
        finally {
            if (null != br) {
                try {
                    br.close();
                }
                catch (IOException e) {
                    log.error("解析http请求数据失败",e);
                }
            }
        }
        return sb.toString();
    }

}
