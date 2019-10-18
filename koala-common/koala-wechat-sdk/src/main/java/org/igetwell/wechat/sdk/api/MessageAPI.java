package org.igetwell.wechat.sdk.api;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.igetwell.common.uitls.HttpClientUtils;
import org.igetwell.wechat.sdk.bean.Message;

import java.nio.charset.Charset;

/**
 * 当用户主动发消息给公众号的时候
 * （包括发送信息、点击自定义菜单click事件、订阅事件、扫描二维码事件、支付成功事件、用户维权），
 * 微信将会把消息数据推送给开发者，
 * 开发者在一段时间内（目前修改为48小时）可以调用客服消息接口，
 * 通过POST一个JSON数据包来发送消息给普通用户，
 * 在48小时内不限制发送次数。
 * 此接口主要用于客服等有人工消息处理环节的功能，方便开发者为用户提供更加优质的服务。
 *
 */
public class MessageAPI extends API {

    /**
     * 消息发送
     *
     * @param access_token access_token
     * @param message  message
     * @return BaseResult
     */
    public static String messageCustomSend(String access_token, String message) {
        HttpUriRequest httpUriRequest = RequestBuilder.post()
                .setHeader(APPLICATION_JSON)
                .setUri(BASE_URI + "/cgi-bin/message/custom/send")
                .addParameter(ACCESS_TOKEN, accessToken(access_token))
                .setEntity(new StringEntity(message, Charset.forName("UTF-8")))
                .build();
        String response = HttpClientUtils.getInstance().sendHttpPost(httpUriRequest.getURI().toString());
        return response;
    }
}
