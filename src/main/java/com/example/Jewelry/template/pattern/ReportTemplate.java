package com.example.Jewelry.template.pattern;

import com.example.Jewelry.template.ReportResult;
import com.example.Jewelry.template.ReportRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public abstract class ReportTemplate {

    private AggregationStrategy aggregationStrategy;
    private final String reportTitle;

    protected ReportTemplate(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public void setAggregationStrategy(AggregationStrategy aggregationStrategy) {
        this.aggregationStrategy = aggregationStrategy;
    }

    public ReportResult generateReport(LocalDate start, LocalDate end) {
        validateRange(start, end);
        List<ReportRow> rawData = loadRawData(start, end);
        ReportResult result = aggregate(rawData, start, end);
        render(result);
        return result;
    }

    protected abstract List<ReportRow> loadRawData(LocalDate start, LocalDate end);

    protected ReportResult aggregate(List<ReportRow> data, LocalDate start, LocalDate end) {
        if (aggregationStrategy == null) {
            throw new IllegalStateException("AggregationStrategy is required.");
        }
        return aggregationStrategy.aggregate(data, reportTitle, start, end);
    }

    protected void render(ReportResult result) {
        // Hook for subclasses. Optional formatting can be applied here.
    }

    protected void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date.");
        }
    }

    protected boolean isWithinRange(LocalDateTime value, LocalDate start, LocalDate end) {
        if (value == null || start == null || end == null) {
            return false;
        }
        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay().minusNanos(1);
        return !value.isBefore(startAt) && !value.isAfter(endAt);
    }
}

