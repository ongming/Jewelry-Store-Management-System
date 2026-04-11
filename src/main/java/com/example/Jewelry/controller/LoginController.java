package com.example.Jewelry.controller;

import com.example.Jewelry.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController {

    private final AccountService accountService;

    public LoginController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping({"/login", "/management/login"})
    public String loginPage() {
        return "management/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        Model model) {
        boolean isSuccess = accountService.login(username, password);
        if (isSuccess) {
            return "redirect:/management/dashboard";
        }

        model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng.");
        model.addAttribute("username", username);
        return "management/login";
    }

    @GetMapping("/management/dashboard")
    public String managementDashboard() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/login")
    public String adminLoginRedirect() {
        return "redirect:/login";
    }

    @GetMapping("/management/db-test")
    @ResponseBody
    public String dbTest() {
        try {
            long totalAccounts = accountService.count();
            return "KET NOI DATABASE OK - Accounts: " + totalAccounts;
        } catch (Exception exception) {
            return "KET NOI DATABASE THAT BAI: " + exception.getMessage();
        }
    }
}
