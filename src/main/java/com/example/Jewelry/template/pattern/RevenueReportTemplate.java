package com.example.Jewelry.template.pattern;

import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.repository.OrderRepository;
import com.example.Jewelry.template.ReportRow;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RevenueReportTemplate extends ReportTemplate {

    private final OrderRepository orderRepository;

    public RevenueReportTemplate(OrderRepository orderRepository) {
        super("Revenue report");
        this.orderRepository = orderRepository;
    }

    @Override
    protected List<ReportRow> loadRawData(LocalDate start, LocalDate end) {
        return orderRepository.findAll().stream()
            .filter(order -> isWithinRange(order.getOrderDate(), start, end))
            .map(this::toRow)
            .collect(Collectors.toList());
    }

    private ReportRow toRow(Order order) {
        String label = order.getOrderNumber();
        return new ReportRow(label, order.getFinalTotal(), 1, order.getOrderDate());
    }
}

