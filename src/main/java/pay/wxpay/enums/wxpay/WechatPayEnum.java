package pay.wxpay.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ankh
 * @Description
 * @createTime 2023/8/11 15:29
 */
@AllArgsConstructor
@Getter
public enum WechatPayEnum {

    /**
     * native
     */
    NATIVE("native"),
    /**
     * app
     */
    APP("app"),
    /**
     * h5
     */
    H5("h5"),
    /**
     *  jsapi
     */
    JSAPI("jsapi"),

    /**
     *  小程序jsapi
     */
    SUB_JSAPI("sub_jsapi");

    private String type;

}
