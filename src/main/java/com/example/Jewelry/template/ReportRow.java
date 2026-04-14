package com.example.Jewelry.template;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReportRow {

    private final String label;
    private final BigDecimal metric;
    private final int count;
    private final LocalDateTime occurredAt;

    public ReportRow(String label, BigDecimal metric, int count, LocalDateTime occurredAt) {
        this.label = label;
        this.metric = metric == null ? BigDecimal.ZERO : metric;
        this.count = count;
        this.occurredAt = occurredAt;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getMetric() {
        return metric;
    }

    public int getCount() {
        return count;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}

