package com.example.Jewelry.event;

import java.time.LocalDate;
import java.util.List;

/**
 * Event payload cho bản tin tong hop gui khach hang hang tuan.
 */
public class WeeklyCustomerDigestEvent {

    private final LocalDate weekStart;
    private final LocalDate weekEnd;
    private final String topProductName;
    private final int topProductQuantity;
    private final List<CustomerDigestMessage> customerMessages;

    public WeeklyCustomerDigestEvent(LocalDate weekStart,
                                     LocalDate weekEnd,
                                     String topProductName,
                                     int topProductQuantity,
                                     List<CustomerDigestMessage> customerMessages) {
        this.weekStart = weekStart;
        this.weekEnd = weekEnd;
        this.topProductName = topProductName;
        this.topProductQuantity = topProductQuantity;
        this.customerMessages = customerMessages;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public LocalDate getWeekEnd() {
        return weekEnd;
    }

    public String getTopProductName() {
        return topProductName;
    }

    public int getTopProductQuantity() {
        return topProductQuantity;
    }

    public List<CustomerDigestMessage> getCustomerMessages() {
        return customerMessages;
    }

    public record CustomerDigestMessage(
        Integer customerId,
        String customerName,
        String customerPhone,
        String message
    ) {
    }
}

