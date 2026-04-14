package com.example.Jewelry.template;

public enum ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    public static ReportPeriod fromString(String value) {
        if (value == null) {
            return DAILY;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "weekly", "week" -> WEEKLY;
            case "monthly", "month" -> MONTHLY;
            case "yearly", "year" -> YEARLY;
            default -> DAILY;
        };
    }
}

