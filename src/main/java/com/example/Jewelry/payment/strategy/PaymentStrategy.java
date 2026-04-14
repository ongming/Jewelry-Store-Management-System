package com.example.Jewelry.payment.strategy;

import com.example.Jewelry.model.entity.Order;

import java.math.BigDecimal;

public interface PaymentStrategy {

    String getMethodCode();

    PaymentExecutionResult execute(Order order, BigDecimal amount, String orderInfo);
}
