package tw.com.ispan.backend.payment.ecpay;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 獨立且彈性的綠界科技 (ECPay) 金流業務邏輯服務。
 * 
 * <p>
 * 本模組完全獨立於特定的訂單系統 (如票券或商品訂單)。它只接收通用的 Map 參數，
 * 生成符合綠界加密規範的 CheckMacValue 簽章及自動提交的 HTML Form 表單。
 * 未來不論是 B2C 購票/購物理財、亦或是 B2B 主辦方年費、會費付款等，皆可直接調用此服務。
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EcpayPaymentService {

    private final EcpayProperties ecpayProperties;

    /**
     * 封裝通用的 AIO 信用卡交易表單生成方法，適用於全站任何需要金流的模組（如票務、商品、年費等）。
     *
     * @param merchantTradeNo     商店交易編號（20碼以內唯一英數字）
     * @param totalAmount         交易金額（整數）
     * @param tradeDesc           交易描述
     * @param itemName            商品名稱
     * @param clientBackUrlSuffix 用戶付款完按「返回商店」的自訂導向後綴 (例如 "?tOrderId=xxxx" 或
     *                            "?mOrderId=xxxx")
     * @return 自動提交表單的 HTML
     */
    public String generateAioCreditForm(
            String merchantTradeNo,
            int totalAmount,
            String tradeDesc,
            String itemName,
            String clientBackUrlSuffix) {
        return generateAioCreditForm(merchantTradeNo, totalAmount, tradeDesc, itemName, clientBackUrlSuffix, null);
    }

    /**
     * 同上，但允許呼叫端直接指定完整的「返回商店」導向網址 (explicitClientBackUrl)，
     * 避免走票務/商品那套 /paymentTicket → /paymentMerch 的字串替換慣例。
     * 供 B2B 主辦方年費等新模組使用。
     *
     * @param explicitClientBackUrl 若非 null，直接作為 ClientBackURL，忽略 clientBackUrlSuffix 的替換規則
     */
    public String generateAioCreditForm(
            String merchantTradeNo,
            int totalAmount,
            String tradeDesc,
            String itemName,
            String clientBackUrlSuffix,
            String explicitClientBackUrl) {

        Map<String, String> params = new HashMap<>();
        params.put("MerchantTradeNo", merchantTradeNo);
        params.put("MerchantTradeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        params.put("TotalAmount", String.valueOf(totalAmount));
        params.put("TradeDesc", tradeDesc);
        params.put("ItemName", itemName);
        params.put("NeedExtraPaidInfo", "N");
        params.put("DeviceSource", "P");
        params.put("ChoosePayment", "Credit");
        params.put("PaymentType", "aio");
        params.put("EncryptType", "1");

        // 使用通用 Properties 中的 Callback/跳轉 URL
        params.put("ReturnURL", ecpayProperties.getReturnUrl());
        params.put("OrderResultURL", ecpayProperties.getOrderResultUrl());

        // 呼叫端若指定完整 ClientBackURL 則優先採用，否則沿用票務/商品的 suffix 慣例
        String backUrl;
        if (explicitClientBackUrl != null && !explicitClientBackUrl.isEmpty()) {
            backUrl = explicitClientBackUrl;
        } else {
            backUrl = ecpayProperties.getClientBackUrl();
            if (clientBackUrlSuffix != null && !clientBackUrlSuffix.isEmpty()) {
                if (clientBackUrlSuffix.contains("mOrderId=")) {
                    backUrl = backUrl.replace("/paymentTicket", "/paymentMerch");
                }
                backUrl += clientBackUrlSuffix;
            }
        }
        params.put("ClientBackURL", backUrl);

        return generatePaymentForm(params);
    }

    /**
     * 產生自動跳轉至綠界科技的 HTML 表單字串。
     *
     * @param params 綠界支付API所需的參數對應表 (如 MerchantTradeNo, TotalAmount, ItemName,
     *               ReturnURL, ClientBackURL等)
     * @return 自動 Submit 導向綠界支付頁面的 HTML `<form>` 字串。
     */
    public String generatePaymentForm(Map<String, String> params) {
        // 確保帶入正確的 MerchantID
        Map<String, String> formParams = new HashMap<>(params);

        formParams.put("MerchantID", ecpayProperties.getMerchantId());

        System.out.println("========== ECPAY PARAMS ==========");
        formParams.forEach((k, v) -> System.out.println(k + " = " + v));
        System.out.println("==================================");
        log.info("ECPAY PARAM = {}", formParams);

        String checkMac = generateCheckMacValue(formParams);

        formParams.put("CheckMacValue", checkMac);

        System.out.println("========== ECPAY PARAM ==========");
        formParams.forEach((key, value) -> {
            System.out.println(key + " = " + value);
        });
        System.out.println("===============================");

        // 2. 組合自動提交 HTML 表單
        StringBuilder html = new StringBuilder();
        html.append(
                String.format("<form id=\"ecpay-form\" method=\"post\" action=\"%s\">", ecpayProperties.getApiUrl()));
        for (Map.Entry<String, String> entry : formParams.entrySet()) {
            html.append(String.format("<input type=\"hidden\" name=\"%s\" value=\"%s\" />",
                    entry.getKey(), entry.getValue()));
        }
        html.append("</form>");
        html.append("<script>document.getElementById('ecpay-form').submit();</script>");

        log.info("【綠界支付】成功生成 HTML 自動跳轉表單，交易編號: {}", formParams.get("MerchantTradeNo"));
        System.out.println(formParams.get("MerchantTradeNo"));
        return html.toString();
    }

    /**
     * 驗證綠界科技回傳通知 (Callback/Webhook) 的簽章是否正確，以防偽造。
     *
     * @param callbackParams 綠界 POST 回傳的所有參數
     * @return 驗證通過返回 true，否則為 false
     */
    public boolean verifyCheckMacValue(Map<String, String> callbackParams) {
        if (callbackParams == null || !callbackParams.containsKey("CheckMacValue")) {
            log.warn("【綠界驗簽失敗】回傳參數不含 CheckMacValue 欄位！");
            return false;
        }

        String receivedCheckMac = callbackParams.get("CheckMacValue");

        // 複製一份參數，排除 CheckMacValue 以重新計算簽章
        Map<String, String> verifyParams = new TreeMap<>(callbackParams);
        verifyParams.remove("CheckMacValue");

        String calculatedCheckMac = generateCheckMacValue(verifyParams);
        boolean isMatched = calculatedCheckMac.equalsIgnoreCase(receivedCheckMac);

        if (!isMatched) {
            log.warn("【綠界驗簽失敗】簽章不符！接收值: {}, 計算值: {}", receivedCheckMac, calculatedCheckMac);
        } else {
            log.info("【綠界驗簽成功】簽章校驗通過！交易編號: {}", callbackParams.get("MerchantTradeNo"));
        }
        return isMatched;
    }

    /**
     * 核心邏輯：生成綠界科技特有的 CheckMacValue 雜湊值。
     * 
     * @param params 需計算簽章的參數對應表
     * @return SHA-256 簽章字串 (大寫)
     */
    public String generateCheckMacValue(Map<String, String> params) {
        // 1. 使用 TreeMap 對 Key 進行排序 (依 A-Z 順序)
        Map<String, String> sortedMap = new TreeMap<>(params);
        sortedMap.remove("CheckMacValue");

        // 2. 組合為 Key1=Value1&Key2=Value2 形式的字串
        String paramString = sortedMap.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        // 3. 前後拼接 HashKey 與 HashIV
        String rawString = "HashKey=" + ecpayProperties.getHashKey() + "&" + paramString + "&HashIV="
                + ecpayProperties.getHashIv();

        // 4. URL 編碼與特殊字元轉換
        String encodedString = ecpayUrlEncode(rawString);

        // 5. 轉為小寫
        String finalRaw = encodedString.toLowerCase();

        // 6. 進行 SHA-256 加密並轉成大寫
        return sha256Hex(finalRaw).toUpperCase();
    }

    /**
     * 符合綠界金流規格的特殊 URL Encode 字元轉換。
     */
    private String ecpayUrlEncode(String str) {

        // 先進行標準的 URL 編碼
        String encoded = URLEncoder.encode(str, StandardCharsets.UTF_8);

        encoded = encoded.toLowerCase();

        // 綠界對特定編碼字元有強制的大小寫要求，請嚴格執行以下替換
        return encoded
                .replace("%21", "!")
                .replace("%2a", "*")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%2d", "-")
                .replace("%2e", ".")
                .replace("%5f", "_")
                .replace("%2b", "+")
                .replace("%20", "+");
    }

    /**
     * 交易字串的 SHA-256 雜湊 Hex。
     */
    private String sha256Hex(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 演算法不可用", e);
        }
    }
}
