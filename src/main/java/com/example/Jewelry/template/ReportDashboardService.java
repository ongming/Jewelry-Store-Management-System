package com.example.Jewelry.template;

import com.example.Jewelry.template.pattern.AggregationStrategy;
import com.example.Jewelry.template.pattern.DailyAggregation;
import com.example.Jewelry.template.pattern.InventoryReportTemplate;
import com.example.Jewelry.template.pattern.MonthlyAggregation;
import com.example.Jewelry.template.pattern.RevenueReportTemplate;
import com.example.Jewelry.template.pattern.TopProductReportTemplate;
import com.example.Jewelry.template.pattern.WeeklyAggregation;
import com.example.Jewelry.template.pattern.YearlyAggregation;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReportDashboardService {

    private final RevenueReportTemplate revenueReportTemplate;
    private final TopProductReportTemplate topProductReportTemplate;
    private final InventoryReportTemplate inventoryReportTemplate;

    public ReportDashboardService(RevenueReportTemplate revenueReportTemplate,
                                  TopProductReportTemplate topProductReportTemplate,
                                  InventoryReportTemplate inventoryReportTemplate) {
        this.revenueReportTemplate = revenueReportTemplate;
        this.topProductReportTemplate = topProductReportTemplate;
        this.inventoryReportTemplate = inventoryReportTemplate;
    }

    public ReportResult getRevenueReport(LocalDate start, LocalDate end, ReportPeriod period) {
        revenueReportTemplate.setAggregationStrategy(resolveStrategy(period));
        return revenueReportTemplate.generateReport(start, end);
    }

    public ReportResult getTopProductReport(LocalDate start, LocalDate end, ReportPeriod period) {
        topProductReportTemplate.setAggregationStrategy(resolveStrategy(period));
        return topProductReportTemplate.generateReport(start, end);
    }

    public ReportResult getInventoryReport(LocalDate start, LocalDate end, ReportPeriod period) {
        inventoryReportTemplate.setAggregationStrategy(resolveStrategy(period));
        return inventoryReportTemplate.generateReport(start, end);
    }

    private AggregationStrategy resolveStrategy(ReportPeriod period) {
        if (period == null) {
            return new DailyAggregation();
        }
        return switch (period) {
            case WEEKLY -> new WeeklyAggregation();
            case MONTHLY -> new MonthlyAggregation();
            case YEARLY -> new YearlyAggregation();
            default -> new DailyAggregation();
        };
    }
}

