package com.example.Jewelry.template;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ReportResult {

    private final String title;
    private final LocalDate start;
    private final LocalDate end;
    private final List<ReportRow> rows;
    private final BigDecimal total;
    private final LocalDateTime generatedAt;

    private ReportResult(String title, LocalDate start, LocalDate end, List<ReportRow> rows, BigDecimal total) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.rows = rows == null ? Collections.emptyList() : rows;
        this.total = total == null ? BigDecimal.ZERO : total;
        this.generatedAt = LocalDateTime.now();
    }

    public static ReportResult fromRows(String title, LocalDate start, LocalDate end, List<ReportRow> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        if (rows != null) {
            for (ReportRow row : rows) {
                if (row != null && row.getMetric() != null) {
                    sum = sum.add(row.getMetric());
                }
            }
        }
        return new ReportResult(title, start, end, rows, sum);
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public List<ReportRow> getRows() {
        return rows;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}