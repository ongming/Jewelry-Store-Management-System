package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.model.entity.Admin;
import com.example.Jewelry.model.entity.OtpVerification;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.model.enums.AccountStatusEnum;
import com.example.Jewelry.factory.AccountFactory;
import com.example.Jewelry.repository.StaffRepository;
import com.example.Jewelry.service.AccountService;
import com.example.Jewelry.service.AdminService;
import com.example.Jewelry.service.OtpService;
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

@Controller
public class AuthController {

    private final AccountService accountService;
    private final StaffService staffService;
    private final AdminService adminService;
    private final StaffRepository staffRepository;
    private final OtpService otpService;

    public AuthController(AccountService accountService,
                          StaffService staffService,
                          AdminService adminService,
                          StaffRepository staffRepository,
                          OtpService otpService) {
        this.accountService = accountService;
        this.staffService = staffService;
        this.adminService = adminService;
        this.staffRepository = staffRepository;
        this.otpService = otpService;
    }

    @GetMapping({"/auth/login", "/login"})
    public String loginPage() {
        return "login";
    }

    @GetMapping({"/management/login", "/admin/login"})
    public String legacyLoginPage() {
        return "redirect:/auth/login";
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

        // Áp dụng State Pattern: Kiểm tra xem trạng thái tài khoản có cho phép đăng nhập không
        if (!account.canLogin()) {
            AccountStatusEnum statusEnum = AccountStatusEnum.fromString(account.getStatus());
            model.addAttribute("error", statusEnum.getIcon() + " " + statusEnum.getDescription());
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

    @GetMapping({"/auth/register", "/register"})
    public String registerPage(Model model) {
        return "register";
    }

    @PostMapping({"/auth/register", "/register"})
    @Transactional
    public String register(@RequestParam String fullName,
                          @RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String passwordConfirm,
                          @RequestParam(required = false) String email,
                          Model model) {
        
        // Kiểm tra các field bắt buộc
        if (fullName == null || fullName.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập họ tên.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }

        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập tên đăng nhập.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập địa chỉ email.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", username);
            return "register";
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            model.addAttribute("error", "Email không hợp lệ.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }

        if (password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập mật khẩu.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }

        // Kiểm tra mật khẩu trùng khớp
        if (!password.equals(passwordConfirm)) {
            model.addAttribute("error", "Mật khẩu xác nhận không trùng khớp.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }

        // Kiểm tra độ dài mật khẩu
        if (password.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }

        // Kiểm tra username có tồn tại
        if (accountService.findByUsername(username.trim()).isPresent()) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.");
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "register";
        }

        try {
            // Gửi OTP đến email
            otpService.sendOtp(email.trim(), fullName.trim(), username.trim(), password.trim());
            
            // Chuyển đến trang xác nhận OTP
            String encodedEmail = URLEncoder.encode(email.trim(), StandardCharsets.UTF_8);
            return "redirect:/auth/verify-otp?email=" + encodedEmail;
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi gửi OTP: " + e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }
    }

    @GetMapping("/auth/verify-otp")
    public String verifyOtpPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/auth/verify-otp")
    @Transactional
    public String verifyOtp(@RequestParam String email,
                           @RequestParam String otpCode,
                           Model model) {
        try {
            // Xác nhận OTP
            if (!otpService.verifyOtp(email.trim(), otpCode.trim())) {
                model.addAttribute("error", "Mã OTP không hợp lệ hoặc đã hết hạn.");
                model.addAttribute("email", email);
                return "verify-otp";
            }

            // Lấy thông tin OTP đã xác nhận
            var otpOptional = otpService.getVerifiedOtp(email.trim());
            if (otpOptional.isEmpty()) {
                model.addAttribute("error", "Không tìm thấy thông tin xác nhận.");
                model.addAttribute("email", email);
                return "verify-otp";
            }

            OtpVerification otp = otpOptional.get();

            // Tạo tài khoản nhân viên mới
            int nextStaffId = staffRepository.findMaxStaffId() + 1;
            Staff staff = (Staff) AccountFactory.createAccount("STAFF");
            
            staff.setFullName(otp.getFullName());
            staff.setUsername(otp.getUsername());
            staff.setPasswordHash(otp.getPasswordHash());
            staff.setStatus("INACTIVE");
            staff.setStaffId(nextStaffId);
            
            staffService.save(staff);

            // Xóa OTP sau khi sử dụng
            otpService.deleteOtp(email.trim());
            
            return "redirect:/auth/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi xác nhận: " + e.getMessage());
            model.addAttribute("email", email);
            return "verify-otp";
        }
    }

    @PostMapping("/auth/resend-otp")
    @ResponseBody
    public String resendOtp(@RequestParam String email) {
        try {
            var otpOptional = otpService.getVerifiedOtp(email.trim());
            if (otpOptional.isEmpty()) {
                return "{\"success\": false, \"message\": \"Email không tồn tại hoặc đã hết hạn\"}";
            }

            OtpVerification otp = otpOptional.get();
            otpService.sendOtp(otp.getEmail(), otp.getFullName(), otp.getUsername(), otp.getPasswordHash());
            
            return "{\"success\": true}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    @GetMapping("/management/dashboard")
    public String legacyManagementDashboard(HttpSession session) {
        Object roleName = session.getAttribute("roleName");
        if ("ADMIN".equals(roleName)) {
            return "redirect:/admin/dashboard";
        }
        if ("STAFF".equals(roleName)) {
            return "redirect:/staff/dashboard";
        }
        return "redirect:/auth/login";
    }

    @GetMapping({"/auth/accounts", "/admin/staff-management"})
    public String accountManagement(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false, defaultValue = "ALL") String role,
                                    @RequestParam(required = false) Integer editId,
                                    @RequestParam(required = false) Integer viewId,
                                    @RequestParam(required = false, defaultValue = "false") boolean create,
                                    HttpSession session,
                                    Model model) {
        Admin currentAdmin = resolveCurrentAdmin(session);
        if (currentAdmin == null) {
            return "redirect:/auth/login";
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = role == null ? "ALL" : role.trim().toUpperCase(Locale.ROOT);
        String effectiveRole = ("ALL".equals(normalizedRole) || "STAFF".equals(normalizedRole))
            ? normalizedRole
            : "STAFF";

        try {
            // Lấy danh sách Staff được quản lý (ACTIVE)
            List<AccountRow> rows = staffRepository.findManagedStaffWithManager(currentAdmin.getAccountId()).stream()
                .map(this::toRow)
                .filter(row -> effectiveRole.equals("ALL") || row.role().equals(effectiveRole))
                .filter(row -> normalizedKeyword.isBlank()
                    || row.fullName().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                    || row.username().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .sorted(Comparator.comparing(AccountRow::accountId))
                .toList();

            // Lấy TẤT CẢ tài khoản INACTIVE
            List<Account> inactiveAccounts = adminService.getAllVisibleInactiveAccounts();
            long activeManagedCount = rows.size();
            long inactiveCount = inactiveAccounts.size();

            AccountRow editRow = editId == null ? null : rows.stream().filter(row -> row.accountId() == editId).findFirst().orElse(null);
            AccountRow viewRow = viewId == null ? null : rows.stream().filter(row -> row.accountId() == viewId).findFirst().orElse(null);

            model.addAttribute("rows", rows);
            model.addAttribute("inactiveAccounts", inactiveAccounts);
            model.addAttribute("activeManagedCount", activeManagedCount);
            model.addAttribute("inactiveCount", inactiveCount);
            model.addAttribute("totalAccounts", activeManagedCount + inactiveCount);
            model.addAttribute("showModal", create || editRow != null);
            model.addAttribute("isEdit", editRow != null);
            model.addAttribute("editRow", editRow);
            model.addAttribute("viewRow", viewRow);
        } catch (RuntimeException runtimeException) {
            model.addAttribute("rows", List.of());
            model.addAttribute("inactiveAccounts", List.of());
            model.addAttribute("showModal", false);
            model.addAttribute("isEdit", false);
            model.addAttribute("editRow", null);
            model.addAttribute("viewRow", null);
            model.addAttribute("error", "Không thể tải danh sách nhân sự: " + runtimeException.getMessage());
        }

        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("role", effectiveRole);
        return "admin/staff-management";
    }

    @PostMapping({"/auth/accounts", "/admin/staff-management"})
    @Transactional
    public String createAccount(@RequestParam String fullName,
                                @RequestParam String username,
                                @RequestParam String password,
                                @RequestParam(defaultValue = "ACTIVE") String status,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Admin currentAdmin = resolveCurrentAdmin(session);
        if (currentAdmin == null) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập đã hết hạn.");
            return "redirect:/auth/login";
        }

        if (accountService.findByUsername(username.trim()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập đã tồn tại.");
            return "redirect:/admin/staff-management?create=true";
        }

        int nextStaffId = staffRepository.findMaxStaffId() + 1;
        
        // Sử dụng AccountFactory thay vì new Staff()
        Staff staff = (Staff) AccountFactory.createAccount("STAFF");
        
        staff.setFullName(fullName.trim());
        staff.setUsername(username.trim());
        staff.setPasswordHash(password.trim());
        // Vai trò đã được khởi tạo tự động trong Factory
        staff.setStatus(normalizeStatus(status));
        staff.setStaffId(nextStaffId);
        staff.setManagerAdmin(currentAdmin);
        staffService.save(staff);

        redirectAttributes.addFlashAttribute("success", "Đã thêm tài khoản thành công.");
        return "redirect:/admin/staff-management";
    }

    @PostMapping({"/auth/accounts/{accountId}/update", "/admin/staff-management/{accountId}/update"})
    @Transactional
    public String updateAccount(@PathVariable Integer accountId,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String password,
                                @RequestParam(defaultValue = "ACTIVE") String status,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Admin currentAdmin = resolveCurrentAdmin(session);
        if (currentAdmin == null) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập đã hết hạn.");
            return "redirect:/auth/login";
        }

        Staff staff = staffRepository.findManagedStaffByAccountId(accountId, currentAdmin.getAccountId()).orElse(null);
        if (staff == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản để cập nhật.");
            return "redirect:/admin/staff-management";
        }

        staff.setFullName(fullName.trim());
        if (password != null && !password.isBlank()) {
            staff.setPasswordHash(password.trim());
        }
        staff.setStatus(normalizeStatus(status));
        staffService.save(staff);

        redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành công.");
        return "redirect:/admin/staff-management";
    }

    @PostMapping({"/auth/accounts/{accountId}/delete", "/admin/staff-management/{accountId}/delete"})
    @Transactional
    public String deleteAccount(@PathVariable Integer accountId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Admin currentAdmin = resolveCurrentAdmin(session);
        if (currentAdmin == null) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập đã hết hạn.");
            return "redirect:/auth/login";
        }

        Staff managedStaff = staffRepository.findManagedStaffByAccountId(accountId, currentAdmin.getAccountId()).orElse(null);
        if (managedStaff == null) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xóa tài khoản này.");
            return "redirect:/admin/staff-management";
        }

        try {
            accountService.deleteById(accountId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa tài khoản.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa tài khoản: " + exception.getMessage());
        }
        return "redirect:/admin/staff-management";
    }

    /**
     * Kích hoạt tài khoản INACTIVE và gán Manager Admin = admin hiện tại
     */
    @PostMapping({"/admin/staff-management/activate-inactive"})
    @Transactional
    public String activateInactiveAccount(@RequestParam Integer staffAccountId,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        Admin currentAdmin = resolveCurrentAdmin(session);
        if (currentAdmin == null) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập đã hết hạn.");
            return "redirect:/auth/login";
        }

        try {
            adminService.activateInactiveStaff(staffAccountId, currentAdmin.getAccountId());
            redirectAttributes.addFlashAttribute("success", "✅ Kích hoạt tài khoản thành công và gán Manager Admin cho tài khoản");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
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

    @GetMapping("/management/db-test")
    @ResponseBody
    public String legacyDbTest() {
        return dbTest();
    }

    private AccountRow toRow(Staff staff) {
        String status = staff.getStatus() == null ? "ACTIVE" : normalizeStatus(staff.getStatus());
        String displayName;
        if ("INACTIVE".equals(status)) {
            displayName = "Chưa kích hoạt";
        } else {
            displayName = AccountStatusEnum.fromString(status).getDisplayName();
        }
        String avatar = "https://ui-avatars.com/api/?name="
            + URLEncoder.encode(staff.getFullName(), StandardCharsets.UTF_8)
            + "&background=1a1a1a&color=d4af37";
        String managerName = staff.getManagerAdmin() == null ? "-" : staff.getManagerAdmin().getFullName();
        return new AccountRow(staff.getAccountId(), staff.getFullName(), staff.getUsername(), "STAFF", status, displayName, avatar, managerName);
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "ACTIVE";
        }
        status = status.trim().toUpperCase();
        if (status.equals("SUSPENDED") || status.equals("LOCKED")) {
            return status;
        }
        if (status.equals("INACTIVE")) {
            return "INACTIVE"; // Dành cho các state cũ hoặc đang thêm chức năng
        }
        return "ACTIVE";
    }

    private Admin resolveCurrentAdmin(HttpSession session) {
        Object accountId = session.getAttribute("accountId");
        if (!(accountId instanceof Number accountIdNumber)) {
            return null;
        }
        return adminService.findById(accountIdNumber.intValue()).orElse(null);
    }

    public record AccountRow(int accountId,
                             String fullName,
                             String username,
                             String role,
                             String status,
                             String statusDisplayName,
                             String avatarUrl,
                             String managerName) {
    }
}