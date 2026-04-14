package com.example.Jewelry.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportController {

    @GetMapping({
        "/admin/reports",
        "/admin/reports/revenue",
        "/admin/reports/top-products",
        "/admin/reports/inventory"
    })
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }
}
