package org.igetwell.wechat.component.service.impl;

import org.dom4j.Element;
import org.igetwell.common.constans.cache.RedisKey;
import org.igetwell.common.uitls.*;
import org.igetwell.common.uitls.aes.WXBizMsgCrypt;
import org.igetwell.wechat.component.service.IWxComponentService;
import org.igetwell.wechat.sdk.api.ComponentAPI;
import org.igetwell.wechat.sdk.api.MessageAPI;
import org.igetwell.wechat.sdk.bean.component.*;
import org.igetwell.wechat.sdk.response.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 第三方平台全网检测和授权业务
 */
@Service
public class WxComponentService implements IWxComponentService {

    private static Logger logger = LoggerFactory.getLogger(WxComponentService.class);

    @Value("${componentAppId}")
    private String componentAppId;

    @Value("${componentAppSecret}")
    private String componentAppSecret;

    @Value("${componentToken}")
    private String componentToken;

    @Value("${encodingAesKey}")
    private String encodingAesKey;

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 获取第三方平台令牌
     * @return
     * @throws Exception
     */
    @Override
    public String getComponentAccessToken() throws Exception {
        ComponentAccessToken componentAccessToken = redisUtils.get(RedisKey.COMPONENT_ACCESS_TOKEN);
        if (StringUtils.isEmpty(componentAccessToken) || StringUtils.isEmpty(componentAccessToken.getComponentAccessToken())) {
            logger.error("[微信开放平台]-从缓存中获取微信开放平台ComponentAccessToken失败.");
            throw new Exception("[微信开放平台]-从缓存中获取微信开放平台ComponentAccessToken失败.");
        }
        return componentAccessToken.getComponentAccessToken();
    }

    /**
     * 是否强制获取第三方平台令牌
     * @param forceRefresh
     * @return
     * @throws Exception
     */
    @Override
    public void getComponentAccessToken(boolean forceRefresh) throws Exception {
        if (forceRefresh){
            oauthToken();
            return;
        }
        boolean bool = redisUtils.exist(RedisKey.COMPONENT_ACCESS_TOKEN);
        if (!bool){
            oauthToken();
            return;
        }
    }

    /**
     * 根据凭证获取第三方平台令牌
     * @throws Exception
     */
    private void oauthToken() throws Exception {
        String componentVerifyTicket = redisUtils.get(RedisKey.COMPONENT_VERIFY_TICKET);
        if(StringUtils.isEmpty(componentVerifyTicket)){
            logger.error("[微信开放平台]-获取微信开放平台验证票据失败.未获取到componentVerifyTicket.");
            throw new Exception("[微信开放平台]-获取微信开放平台验证票据失败.未获取到componentVerifyTicket.");
        }
        if(StringUtils.isEmpty(componentAppId) || StringUtils.isEmpty(componentAppSecret)){
            logger.error("[微信开放平台]-获取微信开放平台令牌失败.未获取到componentAppId.");
            throw new Exception("[微信开放平台]-获取微信开放平台令牌失败.未获取到componentAppId.");
        }
        ComponentAccessToken componentAccessToken = ComponentAPI.oauthToken(componentAppId, componentAppSecret, componentVerifyTicket);
        if (StringUtils.isEmpty(componentAccessToken) || StringUtils.isEmpty(componentAccessToken.getComponentAccessToken())) {
            logger.error("[微信开放平台]-获取微信开放平台ComponentAccessToken失败. {}", GsonUtils.toJson(componentAccessToken));
            throw new Exception("[微信开放平台]-获取微信开放平台ComponentAccessToken失败.");
        }
        redisUtils.set(RedisKey.COMPONENT_ACCESS_TOKEN, componentAccessToken, componentAccessToken.getExpiresIn());
        return;
    }

    /**
     * 微信开放平台处理授权事件的推送
     * @param request
     * @param nonce
     * @param timestamp
     * @param msgSignature
     * @throws Exception
     */
    @Override
    public void processAuthorizeEvent(HttpServletRequest request, String nonce, String timestamp, String msgSignature) throws Exception {
        if (StringUtils.isEmpty(nonce) || StringUtils.isEmpty(timestamp) || StringUtils.isEmpty(msgSignature)) {
            throw new Exception("[微信开放平台]-接收微信服务器回调字符串失败.");
        }
        String xml = IOUtils.readData(request);
        logger.info("第三方平台全网发布-----------------------原始 Xml={}", xml);
        WXBizMsgCrypt pc = new WXBizMsgCrypt(componentToken, encodingAesKey, componentAppId);
        logger.info("第三方平台全网发布-----------------------解密 WXBizMsgCrypt 成功.");
        xml = pc.decryptMsg(msgSignature, timestamp, nonce, xml);
        logger.info("第三方平台全网发布-----------------------解密后 Xml={}", xml);
        processAuthorizationEvent(xml);
    }

    @Override
    public void checkNetwork(HttpServletRequest request, HttpServletResponse response, String nonce, String timestamp, String msgSignature) throws Exception {
        String xml = IOUtils.readData(request);
        logger.info("全网发布接入检测消息反馈开始--------nonce:{}-------timestamp:{}---------msgSignature:{}.");
        WXBizMsgCrypt pc = new WXBizMsgCrypt(componentToken, encodingAesKey, componentAppId);
        xml = pc.decryptMsg(msgSignature, timestamp, nonce, xml);

        Element element = XmlUtils.parseXml(xml);
        String msgType = XmlUtils.elementText(element, "MsgType");
        String toUserName = XmlUtils.elementText(element,"ToUserName");
        String fromUserName = XmlUtils.elementText(element,"FromUserName");

        logger.info("全网发布接入检测--step.1-----------msgType={}----------toUserName={}---------fromUserName={}", msgType, toUserName, fromUserName);
        logger.info("---全网发布接入检测--step.2-----------xml="+xml);
        if(MessageUtils.MESSAGE_EVENT.equals(msgType)){
            logger.info("---全网发布接入检测--step.3-----------事件消息--------");
            String event = XmlUtils.elementText(element,"Event");
            replyEventMessage(response, event, toUserName, fromUserName);
        }else if(MessageUtils.MESSAGE_TEXT.equals(msgType)){
            logger.info("---全网发布接入检测--step.3-----------文本消息--------");
            String content = XmlUtils.elementText(element,"Content");
            processTextMessage(response, content, toUserName, fromUserName);
        }
    }

    /**
     * 保存微信开放平台Ticket
     *
     * @param xml
     */
    private void processAuthorizationEvent(String xml) throws Exception {
        Element element = XmlUtils.parseXml(xml);
        String infoType = XmlUtils.elementText(element,"InfoType");
        if (!StringUtils.isEmpty(infoType)){
            //验证票据component_verify_ticket
            if (infoType.equalsIgnoreCase("component_verify_ticket")){
                String ticket = XmlUtils.elementText(element,"ComponentVerifyTicket");
                logger.info("第三方平台全网发布-----------------------解密后 ComponentVerifyTicket={}", ticket);
                if(!StringUtils.isEmpty(ticket)){
                    //设置10分钟
                    redisUtils.set(RedisKey.COMPONENT_VERIFY_TICKET, ticket, RedisKey.COMPONENT_VERIFY_TICKET_EXPIRE);
                    getComponentAccessToken(false);
                }
            }
            //取消授权
            if (infoType.equalsIgnoreCase("unauthorized")){
            }
            //授权成功或更新授权
            if (infoType.equalsIgnoreCase("authorized") || infoType.equalsIgnoreCase("updateauthorized")){
                String authorizationCode = XmlUtils.elementText(element, "AuthorizationCode");
                String preAuthCode = XmlUtils.elementText(element, "PreAuthCode");
                //long expired =  Long.valueOf(XmlUtils.elementText(element, "AuthorizationCodeExpiredTime"));
                redisUtils.set(RedisKey.COMPONENT_AUTHORIZATION_CODE, authorizationCode, 600);
                redisUtils.set(RedisKey.COMPONENT_PRE_AUTH_CODE, preAuthCode, 600);

            }
        }

    }

    /**
     * 微信全网接入  文本消息
     * @param response
     * @param toUserName
     * @param fromUserName
     */
    private void processTextMessage(HttpServletResponse response, String content, String toUserName, String fromUserName) throws Exception {
        if("TESTCOMPONENT_MSG_TYPE_TEXT".equals(content)){
            String returnContent = content+"_callback";
            replyTextMessage(response,returnContent, toUserName, fromUserName);
        }else if(StringUtils.startsWithIgnoreCase(content, "QUERY_AUTH_CODE")){
            //先回复空串
            output(response, "");
            //接下来客服API再回复一次消息
            //此时 content字符的内容为是 QUERY_AUTH_CODE:adsg5qe4q35
            replyApiTextMessage(content.split(":")[1], fromUserName);
        }
    }



    /**
     * 方法描述: 类型为enevt的时候，拼接
     * @param response
     * @param event
     * @param toUserName  发送接收人
     * @param fromUserName  发送人
     */
    private void replyEventMessage(HttpServletResponse response, String event, String toUserName, String fromUserName) {
        String content = event + "from_callback";
        replyTextMessage(response,content, toUserName, fromUserName);
    }

    /**
     * 回复微信服务器"文本消息"
     * @param response
     * @param content
     * @param toUserName
     * @param fromUserName
     */
    private void replyTextMessage(HttpServletResponse response, String content, String toUserName, String fromUserName) {
        Long createTime = System.currentTimeMillis() / 1000;
        StringBuffer sb = new StringBuffer();
        sb.append("<xml>");
        sb.append("<ToUserName><![CDATA[").append(fromUserName).append("]]></ToUserName>");
        sb.append("<FromUserName><![CDATA[").append(toUserName).append("]]></FromUserName>");
        sb.append("<CreateTime>").append(createTime).append("</CreateTime>");
        sb.append("<MsgType><![CDATA[text]]></MsgType>");
        sb.append("<Content><![CDATA[").append(content).append("]]></Content>");
        sb.append("</xml>");
        try {
            WXBizMsgCrypt pc = new WXBizMsgCrypt(componentToken, encodingAesKey, componentAppId);
            String text = pc.encryptMsg(sb.toString(), createTime.toString(), "easemob");
            output(response, text);
            sb.setLength(0);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }



    private void replyApiTextMessage(String authorizationCode, String fromUserName) throws Exception {
        // 得到微信授权成功的消息后，应该立刻进行处理！！相关信息只会在首次授权的时候推送过来
        logger.info("------step.4----使用客服消息接口回复粉丝----逻辑开始-------------------------");
        String authorizedToken = this.authorize(authorizationCode);
        String msg = authorizationCode + "_from_api";
        Map<String, Object> params = new HashMap<>();
        params.put("touser", fromUserName);
        params.put("msgtype", "text");
        Map<String, Object> text = new HashMap<>();
        text.put("content", msg);
        params.put("text", text);
        String response = MessageAPI.send(authorizedToken, GsonUtils.toJson(params));
        logger.info("api reply message to to wechat whole network test respose = "+ response);
        if (logger.isDebugEnabled()){
            logger.debug("api reply message to to wechat whole network test respose = "+ response);
        }
    }

    /**
     * 开放平台获取预授权码
     * @return
     * @throws Exception
     */
    public String getPreAuthCode() throws Exception{
        logger.info("[微信开放平台]-从缓存中获取预授权码开始.");
        String preAuthCode = redisUtils.get(RedisKey.COMPONENT_PRE_AUTH_CODE);
        if (StringUtils.isEmpty(preAuthCode)) {
            PreAuthAuthorization preAuth = ComponentAPI.preAuthCode(getComponentAccessToken(), componentAppId);
            if (preAuth == null || StringUtils.isEmpty(preAuth.getPreAuthCode())) {
                logger.error("[微信开放平台]-从缓存中获取预授权码失败.");
                throw new Exception("[微信开放平台]-从缓存中获取预授权码失败.");
            }
            redisUtils.set(RedisKey.COMPONENT_PRE_AUTH_CODE, preAuth.getPreAuthCode(), preAuth.getExpiresIn());
            preAuthCode = preAuth.getPreAuthCode();
        }
        logger.info("[微信开放平台]-从缓存中获取预授权码结束. preAuthCode={}.", preAuthCode);
        return preAuthCode;
    }

    /**
     * 授权后回调URI,获取授权码
     */
    public void setAuthCode(String authorizationCode, Long expiresIn) throws Exception {
        logger.info("[微信开放平台]-授权后回调URI, 获取授权码开始.{}", authorizationCode);
        if (StringUtils.isEmpty(authorizationCode) || StringUtils.isEmpty(expiresIn)){
            logger.error("[微信开放平台]-授权后回调URI, 获取授权码失败. 未获取到authorizationCode.");
            throw new Exception("[微信开放平台]-授权后回调URI, 获取授权码失败. 未获取到authorizationCode.");
        }
        redisUtils.set(RedisKey.COMPONENT_AUTHORIZATION_CODE, authorizationCode, expiresIn);
        logger.info("[微信开放平台]-授权后回调URI,获取授权码结束.{}");
    }

    /**
     * 从缓存中获取授权后回调URI的授权码
     */
    public String getAuthCode() throws Exception {
        logger.info("[微信开放平台]-从缓存中获取授权后回调URI的授权码开始.");
        String authorizationCode = redisUtils.get(RedisKey.COMPONENT_AUTHORIZATION_CODE);
        if (StringUtils.isEmpty(authorizationCode)){
            logger.error("[微信开放平台]-从缓存中获取授权后回调URI的授权码失败.");
            throw new Exception("[微信开放平台]-从缓存中获取授权后回调URI的授权码失败.");
        }
        logger.info("[微信开放平台]-从缓存中获取授权后回调URI的授权码结束. authorizationCode={}.", authorizationCode);
        return authorizationCode;
    }

    /**
     * 使用授权码换取授权方令牌
     * @param authorizationCode  授权code
     */
    @Override
    public String authorize(String authorizationCode) throws Exception {
        if (StringUtils.isEmpty(authorizationCode)){
            logger.error("[微信开放平台]-根据授权码获取微信公众号的授权信息失败. 未获取到authorizationCode.");
            throw new Exception("[微信开放平台]-根据授权码获取微信公众号的授权信息失败. 未获取到authorizationCode.");
        }
        Authorization authorization = ComponentAPI.authorized(getComponentAccessToken(), componentAppId, authorizationCode);
        if (authorization == null || StringUtils.isEmpty(authorization.getAuthorizationInfo().getAuthorizerAccessToken()) || StringUtils.isEmpty(authorization.getAuthorizationInfo().getAuthorizerRefreshToken())){
            logger.error("[微信开放平台]-根据授权码获取微信公众号的授权信息失败.");
            throw new Exception("[微信开放平台]-根据授权码获取微信公众号的授权信息失败.");
        }
        redisUtils.set(String.format(RedisKey.COMPONENT_AUTHORIZED_ACCESS_TOKEN, authorization.getAuthorizationInfo().getAuthorizerAppid()), authorization.getAuthorizationInfo(), authorization.getAuthorizationInfo().getExpiresIn());
        //refreshToken(componentAppId, authorizationInfo.getAuthorizationInfo().getAuthorizerAppid(), authorizationInfo.getAuthorizationInfo().getAuthorizerRefreshToken());
        return authorization.getAuthorizationInfo().getAuthorizerAccessToken();
    }

    /**
     * 获取/刷新授权方接口调用令牌
     * @param appId 授权方appId
     * @param refreshToken
     * @return
     */
    @Override
    public void refreshToken(String appId, String refreshToken) throws Exception {
        if (StringUtils.isEmpty(componentAppId) || StringUtils.isEmpty(appId) || StringUtils.isEmpty(refreshToken)) {
            logger.error("[微信开放平台]-获取/刷新微信公众号接口调用令牌失败.请求参数为空.");
            throw new Exception("[微信开放平台]-获取/刷新微信公众号接口调用令牌失败.请求参数为空.");
        }
        ComponentRefreshAccessToken refreshAccessToken = ComponentAPI.refreshToken(getComponentAccessToken(), componentAppId, appId, refreshToken);
        if (StringUtils.isEmpty(refreshAccessToken) || StringUtils.isEmpty(refreshAccessToken.getAuthorizerAccessToken()) || StringUtils.isEmpty(refreshAccessToken.getAuthorizerRefreshToken())) {
            logger.error("[微信开放平台]-获取/刷新微信公众号接口调用令牌失败.");
            throw new Exception("[微信开放平台]-获取/刷新微信公众号接口调用令牌失败.");
        }
        redisUtils.set(String.format(RedisKey.COMPONENT_AUTHORIZED_REFRESH_TOKEN, appId), refreshAccessToken);
    }

    /**
     * 从缓存中获取授权方令牌
     * @param appId
     * @return
     */
    public String getAccessToken(String appId) throws Exception {
        logger.info("[微信开放平台]-从缓存中获取微信公众号接口调用令牌开始.");
        if (StringUtils.isEmpty(appId)) {
            logger.error("[微信开放平台]-从缓存中获取微信公众号接口调用令牌失败. 请求参数appId为空.");
            throw new Exception("[微信开放平台]-从缓存中获取微信公众号接口调用令牌失败. 请求参数appId为空.");
        }
        ComponentAuthorization componentAuthorization = redisUtils.get(String.format(RedisKey.COMPONENT_AUTHORIZED_ACCESS_TOKEN, appId));
        if (componentAuthorization == null || StringUtils.isEmpty(componentAuthorization.getAuthorizerAccessToken()) || StringUtils.isEmpty(componentAuthorization.getAuthorizerRefreshToken())){
            logger.error("[微信开放平台]-从缓存中获取微信公众号接口调用令牌失败.");
            throw new Exception("[微信开放平台]-从缓存中获取微信公众号接口调用令牌失败.");
        }
        logger.info("[微信开放平台]-从缓存中获取微信公众号接口调用令牌结束.");
        return componentAuthorization.getAuthorizerAccessToken();
    }

    /**
     * 获取授权方的帐号基本信息
     * @param appId 授权方appId
     */
    @Override
    public void getAuthorized(String appId) throws Exception {
        if (StringUtils.isEmpty(componentAppId) || StringUtils.isEmpty(appId)) {
            logger.error("[微信开放平台]-获取授权方的帐号基本信息失败.请求参数为空.");
            throw new Exception("[微信开放平台]-获取授权方的帐号基本信息失败.请求参数为空.");
        }
        String str = ComponentAPI.getAuthorized(getComponentAccessToken(), componentAppId, appId);
        System.err.println(str);
    }

    /**
     * 将小程序和公众号授权给第三方平台 获取预授权链接（网页端预授权）
     * 注：auth_type、biz_appid两个字段互斥。
     */
    @Override
    public String createPreAuthUrl(String redirectUri) throws Exception {
        return createPreAuthUrl(redirectUri, null, null);
    }

    /**
     * 将小程序和公众号授权给第三方平台 获取预授权链接（网页端预授权）
     * @param redirectUri
     * @param authType 要授权的帐号类型：1则商户点击链接后，手机端仅展示公众号、2表示仅展示小程序，3表示公众号和小程序都展示。
     *                 如果未指定，则默认小程序和公众号都展示。第三方平台开发者可以使用本字段来控制授权的帐号类型。
     * @param bizAppid 指定授权唯一的小程序或公众号
     * 注：auth_type、biz_appid两个字段互斥。
     * @return
     */
    @Override
    public String createPreAuthUrl(String redirectUri, String authType, String bizAppid) throws Exception {
        return createPreAuthUrl(redirectUri, authType, bizAppid, false);
    }

    /**
     * 将小程序和公众号授权给第三方平台 获取预授权链接（手机端预授权）
     * 注：auth_type、biz_appid两个字段互斥。
     */
    @Override
    public String createMobilePreAuthUrl(String redirectUri) throws Exception {
        return createPreAuthUrl(redirectUri, null, null, true);
    }

    /**
     * 将小程序和公众号授权给第三方平台 获取预授权链接（手机端预授权）
     * @param redirectUri
     * @param authType 要授权的帐号类型：1则商户点击链接后，手机端仅展示公众号、2表示仅展示小程序，3表示公众号和小程序都展示。
     *                 如果未指定，则默认小程序和公众号都展示。第三方平台开发者可以使用本字段来控制授权的帐号类型。
     * @param bizAppid 指定授权唯一的小程序或公众号
     * 注：auth_type、biz_appid两个字段互斥。
     * @return
     */
    @Override
    public String createMobilePreAuthUrl(String redirectUri, String authType, String bizAppid) throws Exception {
        return createPreAuthUrl(redirectUri, authType, bizAppid, true);
    }

    /**
     * 将公众号/小程序绑定到开放平台帐号下
     */
    public boolean bind(String appId) throws Exception {
        logger.info("[微信开放平台]-将公众号/小程序绑定到开放平台帐号开始.");
        if (StringUtils.isEmpty(componentAppId) || StringUtils.isEmpty(appId)) {
            logger.error("[微信开放平台]-将公众号/小程序绑定到开放平台帐号失败. 请求参数appId为空.");
            throw new Exception("[微信开放平台]-将公众号/小程序绑定到开放平台帐号失败. 请求参数appId为空.");
        }
        BaseResponse response = ComponentAPI.bind(getAccessToken(appId), componentAppId, appId);
        if (!response.isSuccess()) {
            throw new Exception("[微信开放平台]-将公众号/小程序绑定到开放平台帐号失败.");
        }
        logger.info("[微信开放平台]-将公众号/小程序绑定到开放平台帐号结束.");
        return true;
    }

    /**
     * 将公众号/小程序从开放平台帐号下解绑
     */
    public boolean unbind(String appId) throws Exception {
        logger.info("[微信开放平台]-将公众号/小程序从开放平台帐号解绑开始.");
        if (StringUtils.isEmpty(componentAppId) || StringUtils.isEmpty(appId)) {
            logger.error("[微信开放平台]-将公众号/小程序从开放平台帐号下解绑失败. 请求参数appId为空.");
            throw new Exception("[微信开放平台]-将公众号/小程序从开放平台帐号解绑失败. 请求参数appId为空.");
        }
        BaseResponse response = ComponentAPI.unbind(getAccessToken(appId), componentAppId, appId);
        if (!response.isSuccess()) {
            throw new Exception("[微信开放平台]-将公众号/小程序从开放平台帐号解绑失败.");
        }
        logger.info("[微信开放平台]-将公众号/小程序从开放平台帐号解绑结束.");
        return true;
    }

    /**
     * 将小程序和公众号授权给第三方平台
     * @param redirectUri
     * @param authType authType 要授权的帐号类型：1则商户点击链接后，手机端仅展示公众号、2表示仅展示小程序，3表示公众号和小程序都展示。
     *                 如果未指定，则默认小程序和公众号都展示。第三方平台开发者可以使用本字段来控制授权的帐号类型。
     * @param bizAppid 指定授权唯一的小程序或公众号
     * @param isMobile 是否手机模式
     *  注：auth_type、biz_appid两个字段互斥。
     * @return
     * @throws Exception
     */
    private String createPreAuthUrl(String redirectUri, String authType, String bizAppid, boolean isMobile) throws Exception{
        if (isMobile){
            return ComponentAPI.createPreAuthUrl(componentAppId, bizAppid, getPreAuthCode(), authType, true, redirectUri);
        } else {
            return ComponentAPI.createPreAuthUrl(componentAppId, bizAppid, getPreAuthCode(), authType, false, redirectUri);
        }
    }



    private void output(HttpServletResponse response, String text){
        try {
            PrintWriter pw = response.getWriter();
            pw.write(text);
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
