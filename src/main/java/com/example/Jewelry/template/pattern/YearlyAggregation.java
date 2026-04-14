package com.example.Jewelry.template.pattern;

import com.example.Jewelry.template.ReportResult;
import com.example.Jewelry.template.ReportRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YearlyAggregation implements AggregationStrategy {

    @Override
    public ReportResult aggregate(List<ReportRow> data, String title, LocalDate start, LocalDate end) {
        Map<Year, List<ReportRow>> grouped = new LinkedHashMap<>();
        if (data != null) {
            for (ReportRow row : data) {
                if (row == null || row.getOccurredAt() == null) {
                    continue;
                }
                Year key = Year.from(row.getOccurredAt());
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
            }
        }

        List<ReportRow> rows = new ArrayList<>();
        for (Map.Entry<Year, List<ReportRow>> entry : grouped.entrySet()) {
            BigDecimal total = BigDecimal.ZERO;
            int count = 0;
            for (ReportRow row : entry.getValue()) {
                if (row.getMetric() != null) {
                    total = total.add(row.getMetric());
                }
                count += row.getCount();
            }
            LocalDate yearStart = entry.getKey().atDay(1);
            rows.add(new ReportRow(entry.getKey().toString(), total, count, yearStart.atStartOfDay()));
        }

        rows.sort(Comparator.comparing(ReportRow::getOccurredAt, Comparator.nullsLast(LocalDateTime::compareTo)));
        return ReportResult.fromRows(title, start, end, rows);
    }
}
