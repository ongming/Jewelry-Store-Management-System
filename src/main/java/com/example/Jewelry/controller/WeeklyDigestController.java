package com.example.Jewelry.controller;

import com.example.Jewelry.service.WeeklyCustomerDigestPublisherService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/weekly-digest")
public class WeeklyDigestController {

    private final WeeklyCustomerDigestPublisherService weeklyCustomerDigestPublisherService;

    public WeeklyDigestController(WeeklyCustomerDigestPublisherService weeklyCustomerDigestPublisherService) {
        this.weeklyCustomerDigestPublisherService = weeklyCustomerDigestPublisherService;
    }

    @PostMapping("/run-now")
    public String runNow(RedirectAttributes redirectAttributes) {
        weeklyCustomerDigestPublisherService.publishDigestForPreviousWeek();
        redirectAttributes.addFlashAttribute("success", "Da kich hoat gui tong ket tuan ngay lap tuc.");
        return "redirect:/staff/dashboard";
    }
}

