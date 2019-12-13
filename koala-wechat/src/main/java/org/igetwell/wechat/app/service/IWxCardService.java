package org.igetwell.wechat.app.service;

import org.igetwell.wechat.sdk.bean.card.MemberCard;
import org.igetwell.wechat.sdk.bean.card.code.consume.CodeConsume;
import org.igetwell.wechat.sdk.bean.card.code.consume.CodeConsumeResponse;
import org.igetwell.wechat.sdk.bean.card.create.WxCardCreate;
import org.igetwell.wechat.sdk.bean.card.white.White;

/**
 * 微信会员卡
 */
public interface IWxCardService {

    /**
     * 创建微信会员卡
     */
    String createCard(WxCardCreate<MemberCard> card) throws Exception;

    /**
     * 创建微信会员卡
     */
    String createCard(String card) throws Exception;

    /**
     * 创建团购券
     */
    String createGroup(String group) throws Exception;

    /**
     * 创建代金券
     * @param cash
     * @return
     */
    String createCash(String cash) throws Exception;

    /**
     * 创建折扣券
     * @param discount
     * @return
     */
    String createDiscount(String discount) throws Exception;

    /**
     * 创建兑换券
     * @param gift
     * @return
     * @throws Exception
     */
    String createGift(String gift) throws Exception;

    /**
     * 创建投放卡券二维码
     * @param cardId
     * @param code
     * @return
     * @throws Exception
     */
    String launch(String cardId, String code) throws Exception;

    /**
     * 线下核销卡券Code
     */
    CodeConsumeResponse consume(CodeConsume consume) throws Exception;

    /**
     * 设置测试白名单
     * @param white
     */
    boolean white(White white) throws Exception;

}