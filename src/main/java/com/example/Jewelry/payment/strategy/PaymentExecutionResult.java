package com.example.Jewelry.payment.strategy;

import com.example.Jewelry.model.entity.Payment;

/**
 * Kết quả chuẩn sau khi Strategy thực thi thanh toán.
 *
 * @param payment                 bản ghi Payment đã gắn method + order
 * @param externalPaymentRequired true nếu cần thao tác ngoài hệ thống (QR/payUrl)
 * @param payUrl                  link mở trang thanh toán (nếu có)
 * @param qrCodeUrl               link ảnh QR (nếu có)
 * @param gatewayOrderId          mã giao dịch phía cổng thanh toán (nếu có)
 * @param orderInfo               nội dung thanh toán gửi qua gateway
 */
public record PaymentExecutionResult(
    Payment payment,
    boolean externalPaymentRequired,
    String payUrl,
    String qrCodeUrl,
    String gatewayOrderId,
    String orderInfo
) {
}
