package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Payment;
import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.payment.strategy.PaymentExecutionResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentService {

    Optional<Payment> findById(Integer id);

    List<Payment> findAll();

    Payment save(Payment entity);

    /**
     * API nghiệp vụ chính cho luồng thanh toán:
     * - resolve đúng strategy theo paymentMethod
     * - execute strategy để tạo Payment và dữ liệu thanh toán ngoài (nếu có)
     * - gắn Payment vào Order để controller quyết định trạng thái tiếp theo
     */
    PaymentExecutionResult applyPaymentForOrder(Order order, String paymentMethod, BigDecimal amount, String orderInfo);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
