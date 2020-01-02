package org.igetwell.wechat.component.web;

import org.igetwell.common.enums.TradeType;
import org.igetwell.common.uitls.IOUtils;
import org.igetwell.common.uitls.ResponseEntity;
import org.igetwell.system.bean.dto.request.WxPayRequest;
import org.igetwell.wechat.BaseController;
import org.igetwell.wechat.component.service.IWxComponentAppService;
import org.igetwell.wechat.app.service.IWxPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/wx/pay")
public class WxPayController extends BaseController {

    @Autowired
    private IWxPayService iWxPayService;
    @Autowired
    private IWxComponentAppService iWxComponentAppService;

    /**
     * 扫码支付
     * @return
     */
    @PostMapping("/scanPay")
    public Map<String,String> scanPay() {
        Map<String,String> packgeMap = iWxPayService.scan("1234567789","GW201807162055","官网费用", "1", "127.0.0.1");
        return packgeMap;
    }


    /**
     * 公众号支付，APP内调起微信支付
     * 微信H5、APP内调起支付
     * @return
     */
    @PostMapping("/preOrder")
    public Map<String, String> preOrder() {
        return iWxPayService.preOrder(TradeType.MWEB, "ojhc61KyGnCepGMIpcZI-YCPce30", "GW201807162055", "官网费用", "1" , "127.0.0.1");
    }


    @PostMapping("/wxPay")
    public ResponseEntity<Map<String, String>> wxPay(@RequestBody WxPayRequest payRequest) {
        Map<String, String> packageMap = iWxPayService.preOrder(payRequest);
        return ResponseEntity.ok(packageMap);
    }


    /**
     * 微信退款
     */
    @PostMapping("/refundPay")
    public ResponseEntity refundPay(String transactionId, String tradeNo, String fee) throws Exception {
        iWxPayService.refundPay(transactionId, tradeNo,"1", "1", fee);
        return ResponseEntity.ok();
    }


    /**
     * 微信支付返回通知
     */
    @PostMapping(value = "/payNotify", produces = {"application/xml"})
    public void payNotify(){
        String xmlStr = IOUtils.readData(request.get());
        String resultXml = iWxPayService.payNotify(xmlStr);
        renderXml(resultXml);
    }

    /**
     * 微信支付返回通知
     */
    @PostMapping(value = "/refundNotify", produces = {"application/xml"})
    public void refundNotify(){
        String xmlStr = IOUtils.readData(request.get());
        String resultXml = iWxPayService.refundNotify(xmlStr);
        renderXml(resultXml);
    }
}
