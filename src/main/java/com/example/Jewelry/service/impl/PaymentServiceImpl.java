package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.model.entity.Payment;
import com.example.Jewelry.payment.strategy.PaymentExecutionResult;
import com.example.Jewelry.payment.strategy.PaymentStrategy;
import com.example.Jewelry.payment.strategy.PaymentStrategyResolver;
import com.example.Jewelry.repository.PaymentRepository;
import com.example.Jewelry.service.PaymentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStrategyResolver paymentStrategyResolver;

    public PaymentServiceImpl(PaymentRepository paymentRepository, PaymentStrategyResolver paymentStrategyResolver) {
        this.paymentRepository = paymentRepository;
        this.paymentStrategyResolver = paymentStrategyResolver;
    }

    @Override
    public Optional<Payment> findById(Integer id) {
        return paymentRepository.findById(id);
    }

    @Override
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Override
    public Payment save(Payment entity) {
        return paymentRepository.save(entity);
    }

    @Override
    public PaymentExecutionResult applyPaymentForOrder(Order order, String paymentMethod, BigDecimal amount, String orderInfo) {
        if (order == null) {
            throw new IllegalArgumentException("Đơn hàng không hợp lệ để thanh toán.");
        }
        // Bước 1: chọn đúng Strategy theo paymentMethod.
        PaymentStrategy strategy = paymentStrategyResolver.resolve(paymentMethod);
        // Bước 2: strategy xử lý nghiệp vụ thanh toán cụ thể (cash/bank/momo).
        PaymentExecutionResult result = strategy.execute(order, amount, orderInfo);
        // Bước 3: liên kết Payment vào Order để controller lưu trạng thái đơn.
        order.setPayment(result.payment());
        return result;
    }

    @Override
    public void deleteById(Integer id) {
        paymentRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return paymentRepository.existsById(id);
    }

    @Override
    public long count() {
        return paymentRepository.count();
    }
}
