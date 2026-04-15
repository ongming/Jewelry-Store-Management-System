package com.example.Jewelry.payment.strategy;

import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CashPaymentStrategy implements PaymentStrategy {

    public static final String METHOD_CODE = "CASH";

    @Override
    public String getMethodCode() {
        return METHOD_CODE;
    }

    @Override
    public PaymentExecutionResult execute(Order order, BigDecimal amount, String orderInfo) {
        // Tiền mặt không cần gọi cổng thanh toán ngoài.
        Payment payment = new Payment();
        payment.setMethod(METHOD_CODE);
        payment.setOrder(order);
        return new PaymentExecutionResult(payment, false, null, null, null, orderInfo);
    }
}
