package com.example.Jewelry.payment.momo;

public record MomoCreatePaymentResponse(
    int resultCode,
    String message,
    String payUrl,
    String deeplink,
    String qrCodeUrl,
    String orderId,
    String requestId
) {
}
