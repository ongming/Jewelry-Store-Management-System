package com.example.Jewelry.payment.strategy;

import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class BankTransferPaymentStrategy implements PaymentStrategy {

    public static final String METHOD_CODE = "BANK_TRANSFER";

    @Value("${vietqr.bank-bin:970436}")
    private String bankBin;

    @Value("${vietqr.account-number:1040683056}")
    private String accountNumber;

    @Value("${vietqr.account-name:}")
    private String accountName;

    @Value("${vietqr.template:compact2}")
    private String template;

    @Override
    public String getMethodCode() {
        return METHOD_CODE;
    }

    @Override
    public PaymentExecutionResult execute(Order order, BigDecimal amount, String orderInfo) {
        // Sinh link QR VietQR để khách chuyển khoản, trạng thái đơn sẽ chờ xác nhận tiền về.
        String amountValue = normalizeAmount(amount);
        String encodedInfo = URLEncoder.encode(orderInfo == null ? "" : orderInfo, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(accountName == null ? "" : accountName, StandardCharsets.UTF_8);
        String qrCodeUrl = "https://img.vietqr.io/image/" + bankBin + "-" + accountNumber + "-" + template
            + ".png?amount=" + amountValue + "&addInfo=" + encodedInfo + "&accountName=" + encodedName;

        Payment payment = new Payment();
        payment.setMethod(METHOD_CODE);
        payment.setOrder(order);
        return new PaymentExecutionResult(payment, true, null, qrCodeUrl, null, orderInfo);
    }

    private String normalizeAmount(BigDecimal amount) {
        BigDecimal normalized = amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
        return normalized.setScale(0, RoundingMode.DOWN).toPlainString();
    }
}
