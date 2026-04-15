package com.example.Jewelry.payment.strategy;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class PaymentStrategyResolver {

    private static final String DEFAULT_METHOD = CashPaymentStrategy.METHOD_CODE;

    private final Map<String, PaymentStrategy> strategyMap = new LinkedHashMap<>();

    public PaymentStrategyResolver(List<PaymentStrategy> strategies) {
        for (PaymentStrategy strategy : strategies) {
            String code = normalize(strategy.getMethodCode());
            if (strategyMap.containsKey(code)) {
                throw new IllegalStateException("Trùng PaymentStrategy cho phương thức: " + code);
            }
            strategyMap.put(code, strategy);
        }
    }

    /**
     * Chọn đúng Strategy theo mã phương thức thanh toán người dùng gửi lên.
     * Nếu không truyền methodCode thì fallback về CASH.
     */
    public PaymentStrategy resolve(String methodCode) {
        String normalized = normalize(methodCode);
        PaymentStrategy strategy = strategyMap.get(normalized);
        if (strategy == null) {
            throw new IllegalArgumentException("Phương thức thanh toán không được hỗ trợ: " + normalized);
        }
        return strategy;
    }

    private String normalize(String methodCode) {
        if (methodCode == null || methodCode.isBlank()) {
            return DEFAULT_METHOD;
        }
        return methodCode.trim().toUpperCase(Locale.ROOT);
    }
}
