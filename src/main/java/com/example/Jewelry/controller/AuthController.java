package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.model.entity.Admin;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.repository.StaffRepository;
import com.example.Jewelry.service.AccountService;
import com.example.Jewelry.service.AdminService;
import com.example.Jewelry.service.StaffService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class AuthController {

    private final AccountService accountService;
    private final StaffService staffService;
    private final AdminService adminService;
    private final StaffRepository staffRepository;

    private final Map<Integer, String> accountStatuses = new ConcurrentHashMap<>();

    public AuthController(AccountService accountService,
                          StaffService staffService,
                          AdminService adminService,
                          StaffRepository staffRepository) {
        this.accountService = accountService;
        this.staffService = staffService;
        this.adminService = adminService;
        this.staffRepository = staffRepository;
    }

    @GetMapping({"/auth/login", "/login"})
    public String loginPage() {
        return "login";
    }

    @PostMapping({"/auth/login", "/login"})
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Account account;
        try {
            account = accountService.login(username, password).orElse(null);
        } catch (RuntimeException runtimeException) {
            model.addAttribute("error", "Không thể xác thực do lỗi dữ liệu. Vui lòng kiểm tra lại DB sau merge.");
            model.addAttribute("username", username);
            return "login";
        }

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
        return "redirect:/home";
    }

    @GetMapping({"/auth/logout", "/logout"})
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }

    @GetMapping({"/auth/accounts", "/admin/staff-management"})
    public String accountManagement(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false, defaultValue = "ALL") String role,
                                    @RequestParam(required = false) Integer editId,
                                    @RequestParam(required = false) Integer viewId,
                                    @RequestParam(required = false, defaultValue = "false") boolean create,
                                    Model model) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = role == null ? "ALL" : role.trim().toUpperCase(Locale.ROOT);

        List<AccountRow> rows = accountService.findAll().stream()
            .map(this::toRow)
            .filter(row -> normalizedRole.equals("ALL") || row.role().equals(normalizedRole))
            .filter(row -> normalizedKeyword.isBlank()
                || row.fullName().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                || row.username().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
            .sorted(Comparator.comparing(AccountRow::accountId))
            .toList();

        AccountRow editRow = editId == null ? null : rows.stream().filter(row -> row.accountId() == editId).findFirst().orElse(null);
        AccountRow viewRow = viewId == null ? null : rows.stream().filter(row -> row.accountId() == viewId).findFirst().orElse(null);

        model.addAttribute("rows", rows);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("role", normalizedRole);
        model.addAttribute("showModal", create || editRow != null);
        model.addAttribute("isEdit", editRow != null);
        model.addAttribute("editRow", editRow);
        model.addAttribute("viewRow", viewRow);
        return "admin/staff-management";
    }

    @PostMapping({"/auth/accounts", "/admin/staff-management"})
    @Transactional
    public String createAccount(@RequestParam String fullName,
                                @RequestParam String username,
                                @RequestParam String password,
                                @RequestParam String role,
                                @RequestParam(defaultValue = "ACTIVE") String status,
                                RedirectAttributes redirectAttributes) {
        String normalizedRole = normalizeRole(role);
        if (accountService.findByUsername(username.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập đã tồn tại.");
            return "redirect:/admin/staff-management?create=true";
        }

        int nextStaffId = staffRepository.findMaxStaffId() + 1;
        if ("ADMIN".equals(normalizedRole)) {
            Admin admin = new Admin();
            admin.setFullName(fullName.trim());
            admin.setUsername(username.trim());
            admin.setPasswordHash(password.trim());
            admin.setRoleName("ADMIN");
            admin.setStaffId(nextStaffId);
            admin.setManagerAdmin(null);
            adminService.save(admin);
            accountStatuses.put(admin.getAccountId(), normalizeStatus(status));
        } else {
            Staff staff = new Staff();
            staff.setFullName(fullName.trim());
            staff.setUsername(username.trim());
            staff.setPasswordHash(password.trim());
            staff.setRoleName("STAFF");
            staff.setStaffId(nextStaffId);
            staff.setManagerAdmin(getPrimaryAdmin());
            staffService.save(staff);
            accountStatuses.put(staff.getAccountId(), normalizeStatus(status));
        }

        redirectAttributes.addFlashAttribute("success", "Đã thêm tài khoản thành công.");
        return "redirect:/admin/staff-management";
    }

    @PostMapping({"/auth/accounts/{accountId}/update", "/admin/staff-management/{accountId}/update"})
    @Transactional
    public String updateAccount(@PathVariable Integer accountId,
                                @RequestParam String fullName,
                                @RequestParam String password,
                                @RequestParam String role,
                                @RequestParam(defaultValue = "ACTIVE") String status,
                                RedirectAttributes redirectAttributes) {
        Account account = accountService.findById(accountId).orElse(null);
        if (account == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản để cập nhật.");
            return "redirect:/admin/staff-management";
        }

        String normalizedRole = normalizeRole(role);
        String currentRole = normalizeRole(account.getRoleName());
        if (!normalizedRole.equals(currentRole)) {
            redirectAttributes.addFlashAttribute("error", "Không thể đổi vai trò trong phiên bản này.");
            return "redirect:/admin/staff-management?editId=" + accountId;
        }

        account.setFullName(fullName.trim());
        if (password != null && !password.isBlank()) {
            account.setPasswordHash(password.trim());
        }
        accountService.save(account);
        accountStatuses.put(accountId, normalizeStatus(status));

        redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành công.");
        return "redirect:/admin/staff-management";
    }

    @PostMapping({"/auth/accounts/{accountId}/delete", "/admin/staff-management/{accountId}/delete"})
    @Transactional
    public String deleteAccount(@PathVariable Integer accountId,
                                RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteById(accountId);
            accountStatuses.remove(accountId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa tài khoản.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa tài khoản: " + exception.getMessage());
        }
        return "redirect:/admin/staff-management";
    }

    @GetMapping("/auth/db-test")
    @ResponseBody
    public String dbTest() {
        try {
            long totalAccounts = accountService.count();
            return "KET NOI DATABASE OK - Accounts: " + totalAccounts;
        } catch (Exception exception) {
            return "KET NOI DATABASE THAT BAI: " + exception.getMessage();
        }
    }

    private AccountRow toRow(Account account) {
        String role = normalizeRole(account.getRoleName());
        String status = accountStatuses.computeIfAbsent(account.getAccountId(), key -> "ACTIVE");
        String avatar = "https://ui-avatars.com/api/?name="
            + URLEncoder.encode(account.getFullName(), StandardCharsets.UTF_8)
            + "&background=1a1a1a&color=d4af37";
        return new AccountRow(account.getAccountId(), account.getFullName(), account.getUsername(), role, status, avatar);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "STAFF";
        }
        return "ADMIN".equalsIgnoreCase(role.trim()) ? "ADMIN" : "STAFF";
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "ACTIVE";
        }
        return "INACTIVE".equalsIgnoreCase(status.trim()) ? "INACTIVE" : "ACTIVE";
    }

    private Admin getPrimaryAdmin() {
        return adminService.findAll().stream().findFirst().orElse(null);
    }

    public record AccountRow(int accountId,
                             String fullName,
                             String username,
                             String role,
                             String status,
                             String avatarUrl) {
    }
}
