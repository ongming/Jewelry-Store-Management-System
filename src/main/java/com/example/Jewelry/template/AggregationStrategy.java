package com.example.Jewelry.template;

import java.time.LocalDate;
import java.util.List;

public interface AggregationStrategy {

    ReportResult aggregate(List<ReportRow> data, String title, LocalDate start, LocalDate end);
}
