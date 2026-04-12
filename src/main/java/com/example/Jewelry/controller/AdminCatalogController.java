package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.model.entity.Admin;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.repository.StaffRepository;
import com.example.Jewelry.service.AccountService;
import com.example.Jewelry.service.AdminService;
import com.example.Jewelry.service.CategoryService;
import com.example.Jewelry.service.ProductService;
import com.example.Jewelry.service.StaffService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/admin")
public class AdminCatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final StaffService staffService;
    private final AdminService adminService;
    private final StaffRepository staffRepository;

    public AdminCatalogController(ProductService productService,
                                  CategoryService categoryService,
                                  AccountService accountService,
                                  StaffService staffService,
                                  AdminService adminService,
                                  StaffRepository staffRepository) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.accountService = accountService;
        this.staffService = staffService;
        this.adminService = adminService;
        this.staffRepository = staffRepository;
    }

    @GetMapping("/products")
    public String productList(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String productCreateForm(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isEdit", false);
        return "admin/product-form";
    }

    @PostMapping("/products")
    public String createProduct(@RequestParam String productCode,
                                @RequestParam String productName,
                                @RequestParam BigDecimal basePrice,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) MultipartFile imageFile,
                                @RequestParam Integer categoryId,
                                RedirectAttributes redirectAttributes) {
        try {
            String finalImageUrl = resolveImageUrl(imageUrl, imageFile);
            productService.createProduct(productCode, productName, basePrice, finalImageUrl, categoryId);
            redirectAttributes.addFlashAttribute("success", "Thêm sản phẩm thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/products/new";
        } catch (IOException ioException) {
            redirectAttributes.addFlashAttribute("error", "Không thể tải ảnh lên: " + ioException.getMessage());
            return "redirect:/admin/products/new";
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String productEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return productService.findById(id)
            .map(product -> {
                model.addAttribute("product", product);
                model.addAttribute("categories", categoryService.findAll());
                model.addAttribute("isEdit", true);
                return "admin/product-form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm.");
                return "redirect:/admin/products";
            });
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Integer id,
                                @RequestParam String productCode,
                                @RequestParam String productName,
                                @RequestParam BigDecimal basePrice,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) MultipartFile imageFile,
                                @RequestParam Integer categoryId,
                                RedirectAttributes redirectAttributes) {
        try {
            String finalImageUrl = resolveImageUrl(imageUrl, imageFile);
            if (finalImageUrl == null || finalImageUrl.isBlank()) {
                finalImageUrl = productService.findById(id)
                    .map(product -> product.getImageUrl())
                    .orElse(null);
            }
            productService.updateProduct(id, productCode, productName, basePrice, finalImageUrl, categoryId);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sản phẩm thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        } catch (IOException ioException) {
            redirectAttributes.addFlashAttribute("error", "Không thể tải ảnh lên: " + ioException.getMessage());
            return "redirect:/admin/products/" + id + "/edit";
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/categories")
    public String categoryList(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories";
    }

    @GetMapping("/categories/new")
    public String categoryCreateForm(Model model) {
        model.addAttribute("isEdit", false);
        return "admin/category-form";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String categoryName, RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(categoryName);
            redirectAttributes.addFlashAttribute("success", "Thêm danh mục thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/categories/new";
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/{id}/edit")
    public String categoryEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return categoryService.findById(id)
            .map(category -> {
                model.addAttribute("category", category);
                model.addAttribute("isEdit", true);
                return "admin/category-form";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy danh mục.");
                return "redirect:/admin/categories";
            });
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(@PathVariable Integer id,
                                 @RequestParam String categoryName,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, categoryName);
            redirectAttributes.addFlashAttribute("success", "Cập nhật danh mục thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
            return "redirect:/admin/categories/" + id + "/edit";
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Xóa danh mục thành công.");
        } catch (RuntimeException runtimeException) {
            redirectAttributes.addFlashAttribute("error", runtimeException.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/staff-management")
    public String managementPage(@RequestParam(required = false) String keyword,
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

    @PostMapping("/staff-management")
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
            admin.setStatus(normalizeStatus(status));
            admin.setStaffId(nextStaffId);
            admin.setManagerAdmin(null);
            adminService.save(admin);
        } else {
            Staff staff = new Staff();
            staff.setFullName(fullName.trim());
            staff.setUsername(username.trim());
            staff.setPasswordHash(password.trim());
            staff.setRoleName("STAFF");
            staff.setStatus(normalizeStatus(status));
            staff.setStaffId(nextStaffId);
            staff.setManagerAdmin(getPrimaryAdmin());
            staffService.save(staff);
        }

        redirectAttributes.addFlashAttribute("success", "Đã thêm tài khoản thành công.");
        return "redirect:/admin/staff-management";
    }

    @PostMapping("/staff-management/{accountId}/update")
    @Transactional
    public String updateAccount(@PathVariable Integer accountId,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String password,
                                @RequestParam String role,
                                @RequestParam(required = false, defaultValue = "ACTIVE") String status,
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
        account.setStatus(normalizeStatus(status));
        accountService.save(account);

        redirectAttributes.addFlashAttribute("success", "Cập nhật tài khoản thành công.");
        return "redirect:/admin/staff-management";
    }

    @PostMapping("/staff-management/{accountId}/delete")
    @Transactional
    public String deleteAccount(@PathVariable Integer accountId,
                                RedirectAttributes redirectAttributes) {
        try {
            accountService.deleteById(accountId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa tài khoản.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa tài khoản: " + exception.getMessage());
        }
        return "redirect:/admin/staff-management";
    }

    private AccountRow toRow(Account account) {
        String role = normalizeRole(account.getRoleName());
        String status = account.getStatus() != null ? account.getStatus() : "ACTIVE";
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

    private String resolveImageUrl(String imageUrl, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String originalName = imageFile.getOriginalFilename();
            String extension = extractExtension(originalName);
            String fileName = UUID.randomUUID() + extension;

            Path uploadDir = Paths.get("uploads", "products");
            Files.createDirectories(uploadDir);
            Files.copy(imageFile.getInputStream(), uploadDir.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + fileName;
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl.trim();
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            return ".jpg";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (extension.length() > 10) {
            return ".jpg";
        }
        return extension;
    }

    public record AccountRow(int accountId,
                             String fullName,
                             String username,
                             String role,
                             String status,
                             String avatarUrl) {
    }
}