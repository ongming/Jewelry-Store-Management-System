package com.example.Jewelry.payment.strategy;

import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.Payment;
import com.example.Jewelry.payment.momo.MomoCreatePaymentResponse;
import com.example.Jewelry.payment.momo.MomoSandboxClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MomoVnpayPaymentStrategy implements PaymentStrategy {

    public static final String METHOD_CODE = "MOMO_VNPAY";

    private final MomoSandboxClient momoSandboxClient;

    public MomoVnpayPaymentStrategy(MomoSandboxClient momoSandboxClient) {
        this.momoSandboxClient = momoSandboxClient;
    }

    @Override
    public String getMethodCode() {
        return METHOD_CODE;
    }

    @Override
    public PaymentExecutionResult execute(Order order, BigDecimal amount, String orderInfo) {
        // Gọi MoMo sandbox để lấy payUrl/qrCodeUrl cho thanh toán online.
        MomoCreatePaymentResponse response = momoSandboxClient.createPayment(order.getOrderNumber(), amount, orderInfo);

        Payment payment = new Payment();
        payment.setMethod(METHOD_CODE);
        payment.setOrder(order);
        return new PaymentExecutionResult(
            payment,
            true,
            response.payUrl(),
            response.qrCodeUrl(),
            response.orderId(),
            orderInfo
        );
    }
}
