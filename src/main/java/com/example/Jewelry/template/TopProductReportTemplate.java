package com.example.Jewelry.template;

import com.example.Jewelry.model.entity.OrderDetail;
import com.example.Jewelry.repository.OrderDetailRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TopProductReportTemplate extends ReportTemplate {

    private final OrderDetailRepository orderDetailRepository;

    public TopProductReportTemplate(OrderDetailRepository orderDetailRepository) {
        super("Top product report");
        this.orderDetailRepository = orderDetailRepository;
    }

    @Override
    protected List<ReportRow> loadRawData(LocalDate start, LocalDate end) {
        return orderDetailRepository.findAll().stream()
            .filter(detail -> detail.getOrder() != null)
            .filter(detail -> isWithinRange(detail.getOrder().getOrderDate(), start, end))
            .map(this::toRow)
            .collect(Collectors.toList());
    }

    @Override
    protected ReportResult aggregate(List<ReportRow> data, LocalDate start, LocalDate end) {
        Map<String, List<ReportRow>> grouped = new LinkedHashMap<>();
        if (data != null) {
            for (ReportRow row : data) {
                String key = row.getLabel() == null ? "Unknown" : row.getLabel();
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
            }
        }

        List<ReportRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<ReportRow>> entry : grouped.entrySet()) {
            BigDecimal total = BigDecimal.ZERO;
            int count = 0;
            for (ReportRow row : entry.getValue()) {
                if (row.getMetric() != null) {
                    total = total.add(row.getMetric());
                }
                count += row.getCount();
            }
            rows.add(new ReportRow(entry.getKey(), total, count, null));
        }

        rows.sort(Comparator.comparing(ReportRow::getMetric).reversed());
        return ReportResult.fromRows("Top product report", start, end, rows);
    }

    private ReportRow toRow(OrderDetail detail) {
        String label = detail.getProduct() == null ? "Unknown" : detail.getProduct().getProductName();
        BigDecimal revenue = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
        return new ReportRow(label, revenue, detail.getQuantity(), detail.getOrder().getOrderDate());
    }
}
