package com.example.Jewelry.template.pattern;

import com.example.Jewelry.template.ReportResult;
import com.example.Jewelry.template.ReportRow;

import java.time.LocalDate;
import java.util.List;

public interface AggregationStrategy {

    ReportResult aggregate(List<ReportRow> data, String title, LocalDate start, LocalDate end);
}

