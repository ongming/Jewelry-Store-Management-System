package com.example.Jewelry.payment.strategy;

import com.example.Jewelry.model.entity.Order;

import java.math.BigDecimal;

public interface PaymentStrategy {

    /**
     * Trả về mã phương thức thanh toán mà strategy xử lý.
     * Ví dụ: CASH, BANK_TRANSFER, MOMO_VNPAY.
     */
    String getMethodCode();

    /**
     * Thực thi luồng thanh toán cho đơn hàng theo đúng kênh tương ứng.
     *
     * @param order     đơn hàng cần thanh toán
     * @param amount    số tiền cần thu
     * @param orderInfo nội dung hiển thị/chuyển cho cổng thanh toán
     * @return kết quả thực thi, gồm Payment entity và dữ liệu QR/payUrl nếu cần
     */
    PaymentExecutionResult execute(Order order, BigDecimal amount, String orderInfo);
}
