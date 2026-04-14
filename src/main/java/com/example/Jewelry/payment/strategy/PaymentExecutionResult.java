package com.example.Jewelry.payment.strategy;

import com.example.Jewelry.model.entity.Payment;

public record PaymentExecutionResult(
    Payment payment,
    boolean externalPaymentRequired,
    String payUrl,
    String qrCodeUrl,
    String gatewayOrderId,
    String orderInfo
) {
}
