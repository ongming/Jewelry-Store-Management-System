package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.service.AccountService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Locale;

@Controller
public class LoginController {

    private final AccountService accountService;

    public LoginController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/management/login")
    public String legacyLoginPage() {
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Account account = accountService.login(username, password).orElse(null);
        if (account == null) {
            model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng.");
            model.addAttribute("username", username);
            return "login";
        }

        String roleName = account.getRoleName() == null ? "STAFF" : account.getRoleName().toUpperCase(Locale.ROOT);
        session.setAttribute("accountId", account.getAccountId());
        session.setAttribute("username", account.getUsername());
        session.setAttribute("fullName", account.getFullName());
        session.setAttribute("roleName", roleName);

        if ("ADMIN".equals(roleName)) {
            return "redirect:/admin/dashboard";
        }
        if ("STAFF".equals(roleName)) {
            return "redirect:/staff/dashboard";
        }
        return "redirect:/guest/home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/management/dashboard")
    public String managementDashboard(HttpSession session) {
        Object roleName = session.getAttribute("roleName");
        if ("ADMIN".equals(roleName)) {
            return "redirect:/admin/dashboard";
        }
        if ("STAFF".equals(roleName)) {
            return "redirect:/staff/dashboard";
        }
        return "redirect:/guest/home";
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
