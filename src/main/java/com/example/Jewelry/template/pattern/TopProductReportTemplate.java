package com.example.Jewelry.template.pattern;

import com.example.Jewelry.model.entity.OrderDetail;
import com.example.Jewelry.repository.OrderDetailRepository;
import com.example.Jewelry.template.ReportRow;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    private ReportRow toRow(OrderDetail detail) {
        String label = detail.getProduct() == null ? "Unknown" : detail.getProduct().getProductName();
        BigDecimal revenue = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
        return new ReportRow(label, revenue, detail.getQuantity(), detail.getOrder().getOrderDate());
    }
}

