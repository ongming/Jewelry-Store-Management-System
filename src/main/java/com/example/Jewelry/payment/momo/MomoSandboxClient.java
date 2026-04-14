package com.example.Jewelry.payment.momo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class MomoSandboxClient {

    private static final String REQUEST_TYPE = "payWithMethod";
    private static final String HMAC_ALGO = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.redirect-url}")
    private String redirectUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    public MomoSandboxClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public MomoCreatePaymentResponse createPayment(String orderId, BigDecimal amount, String orderInfo) {
        try {
            String normalizedAmount = normalizeAmount(amount);
            String normalizedOrderInfo = (orderInfo == null || orderInfo.isBlank()) ? "pay with MoMo" : orderInfo;
            String requestId = orderId + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String extraData = "";
            String orderGroupId = "";
            boolean autoCapture = true;
            String lang = "vi";

            String rawSignature = "accessKey=" + accessKey
                + "&amount=" + normalizedAmount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + normalizedOrderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + REQUEST_TYPE;
            String signature = hmacSha256Hex(secretKey, rawSignature);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("partnerCode", partnerCode);
            payload.put("partnerName", "Test");
            payload.put("storeId", "MomoTestStore");
            payload.put("requestId", requestId);
            payload.put("amount", normalizedAmount);
            payload.put("orderId", orderId);
            payload.put("orderInfo", normalizedOrderInfo);
            payload.put("redirectUrl", redirectUrl);
            payload.put("ipnUrl", ipnUrl);
            payload.put("lang", lang);
            payload.put("requestType", REQUEST_TYPE);
            payload.put("autoCapture", autoCapture);
            payload.put("extraData", extraData);
            payload.put("orderGroupId", orderGroupId);
            payload.put("signature", signature);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            MomoCreatePaymentResponse momoResponse = objectMapper.readValue(response.body(), MomoCreatePaymentResponse.class);

            if (response.statusCode() >= 400) {
                throw new IllegalStateException("MoMo sandbox trả HTTP " + response.statusCode() + ".");
            }
            if (momoResponse.resultCode() != 0) {
                String message = momoResponse.message() == null ? "Không tạo được giao dịch MoMo." : momoResponse.message();
                throw new IllegalStateException("MoMo lỗi: " + message + " (code=" + momoResponse.resultCode() + ")");
            }
            return momoResponse;
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo thanh toán MoMo sandbox: " + ex.getMessage(), ex);
        }
    }

    private String normalizeAmount(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
        return normalized.setScale(0, RoundingMode.DOWN).toPlainString();
    }

    private String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", value));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo chữ ký HMAC SHA256.", ex);
        }
    }
}
