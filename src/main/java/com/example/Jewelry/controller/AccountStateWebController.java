package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.model.entity.Admin;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.service.AccountService;
import com.example.Jewelry.service.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller để quản lý State Pattern trên web
 */
@Controller
@RequestMapping("/admin/account-state")
public class AccountStateWebController {

    private final AccountService accountService;
    private final AdminService adminService;

    public AccountStateWebController(AccountService accountService, AdminService adminService) {
        this.accountService = accountService;
        this.adminService = adminService;
    }

    /**
     * Hiển thị dashboard quản lý trạng thái tài khoản
     */
    @GetMapping("/dashboard")
    public String stateDashboard(HttpSession session, Model model) {
        // Kiểm tra xem user có phải admin không
        Object roleName = session.getAttribute("roleName");
        if (!"ADMIN".equals(roleName)) {
            return "redirect:/auth/login";
        }

        // Lấy danh sách tất cả accounts
        List<Account> accounts = accountService.findAll();
        
        // Tính toán thống kê
        long activeCount = accounts.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count();
        long suspendedCount = accounts.stream().filter(a -> "SUSPENDED".equals(a.getStatus())).count();
        long lockedCount = accounts.stream().filter(a -> "LOCKED".equals(a.getStatus())).count();
        long inactiveCount = accounts.stream().filter(a -> "INACTIVE".equals(a.getStatus())).count();

        model.addAttribute("accounts", accounts);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("suspendedCount", suspendedCount);
        model.addAttribute("lockedCount", lockedCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("totalAccounts", accounts.size());

        return "admin/account-state-dashboard";
    }

    /**
     * Hiển thị danh sách accounts theo trạng thái
     */
    @GetMapping("/by-status")
    public String accountsByStatus(@RequestParam(defaultValue = "ACTIVE") String status,
                                   HttpSession session,
                                   Model model) {
        Object roleName = session.getAttribute("roleName");
        if (!"ADMIN".equals(roleName)) {
            return "redirect:/auth/login";
        }

        List<Account> accounts = accountService.findByStatus(status);
        
        model.addAttribute("accounts", accounts);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statusName", getStatusDisplayName(status));

        return "admin/account-state-list";
    }

    /**
     * Hiển thị trang quản lý state cho một tài khoản cụ thể
     */
    @GetMapping("/manage/{accountId}")
    public String manageAccountState(@RequestParam Integer accountId,
                                     HttpSession session,
                                     Model model) {
        Object roleName = session.getAttribute("roleName");
        if (!"ADMIN".equals(roleName)) {
            return "redirect:/auth/login";
        }

        Account account = accountService.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        model.addAttribute("account", account);
        model.addAttribute("currentState", account.getAccountStateName());
        model.addAttribute("canLogin", account.canLogin());
        model.addAttribute("canAccessSystem", account.canAccessSystem());
        model.addAttribute("canModifyData", account.canModifyData());

        return "admin/account-state-manage";
    }

    /**
     * Tạm khóa tài khoản (Web Form)
     */
    @PostMapping("/suspend")
    public String suspendAccount(@RequestParam Integer accountId,
                                 HttpSession session,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Object roleName = session.getAttribute("roleName");
        if (!"ADMIN".equals(roleName)) {
            return "redirect:/auth/login";
        }

        try {
            accountService.suspendAccount(accountId);
            redirectAttributes.addFlashAttribute("success", "✅ Tài khoản đã được tạm khóa thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/account-state/dashboard";
    }

    /**
     * Kích hoạt tài khoản (Web Form)
     */
    @PostMapping("/activate")
    public String activateAccount(@RequestParam Integer accountId,
                                  HttpSession session,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Object roleName = session.getAttribute("roleName");
        if (!"ADMIN".equals(roleName)) {
            return "redirect:/auth/login";
        }

        try {
            accountService.activateAccount(accountId);
            redirectAttributes.addFlashAttribute("success", "✅ Tài khoản đã được kích hoạt thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/account-state/dashboard";
    }

    /**
     * Khóa tài khoản (Web Form)
     */
    @PostMapping("/lock")
    public String lockAccount(@RequestParam Integer accountId,
                              HttpSession session,
                              org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Object roleName = session.getAttribute("roleName");
        if (!"ADMIN".equals(roleName)) {
            return "redirect:/auth/login";
        }

        try {
            accountService.lockAccount(accountId);
            redirectAttributes.addFlashAttribute("success", "✅ Tài khoản đã được khóa");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/account-state/dashboard";
    }

    /**
     * Hiển thị danh sách Staff mà Admin quản lý + TẤT CẢ tài khoản INACTIVE
     * Admin sẽ thấy: 
     * - Staff của mình (ACTIVE)
     * - Tất cả tài khoản INACTIVE (bất kể là Staff hay Admin)
     */
    @GetMapping("/staff-management")
    public String staffManagement(HttpSession session, Model model) {
        Object roleName = session.getAttribute("roleName");
        if (!"ADMIN".equals(roleName)) {
            return "redirect:/auth/login";
        }

        Integer adminAccountId = (Integer) session.getAttribute("accountId");
        if (adminAccountId == null) {
            return "redirect:/auth/login";
        }

        // Lấy danh sách Staff mà Admin quản lý (ACTIVE)
        List<Staff> visibleStaffs = adminService.getVisibleStaff(adminAccountId);
        
        // Lấy TẤT CẢ tài khoản INACTIVE (bất kể role)
        List<Account> allInactiveAccounts = adminService.getAllVisibleInactiveAccounts();

        // Phân loại
        long activeManagedCount = visibleStaffs.stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .count();
        long inactiveCount = allInactiveAccounts.size();

        model.addAttribute("staffs", visibleStaffs);
        model.addAttribute("inactiveAccounts", allInactiveAccounts);
        model.addAttribute("activeManagedCount", activeManagedCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("totalAccounts", activeManagedCount + inactiveCount);

        return "admin/inactive-staff-management";
    }

    /**
     * Kích hoạt Staff INACTIVE và gán Manager Admin = admin hiện tại
     */
    @PostMapping("/activate-inactive-staff")
    public String activateInactiveStaff(@RequestParam Integer staffAccountId,
                                        HttpSession session,
                                        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Object roleName = session.getAttribute("roleName");
        if (!"ADMIN".equals(roleName)) {
            return "redirect:/auth/login";
        }

        Integer adminAccountId = (Integer) session.getAttribute("accountId");
        if (adminAccountId == null) {
            return "redirect:/auth/login";
        }

        try {
            adminService.activateInactiveStaff(staffAccountId, adminAccountId);
            redirectAttributes.addFlashAttribute("success", "✅ Kích hoạt Staff thành công và gán Manager Admin cho Staff");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/account-state/staff-management";
    }

    /**
     * Lấy tên hiển thị của status
     */
    private String getStatusDisplayName(String status) {
        return switch (status.toUpperCase()) {
            case "ACTIVE" -> "Hoạt động";
            case "SUSPENDED" -> "Tạm khóa";
            case "LOCKED" -> "Khóa";
            case "INACTIVE" -> "Chưa kích hoạt";
            default -> status;
        };
    }
}
