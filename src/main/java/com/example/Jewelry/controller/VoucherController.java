package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Voucher;
import com.example.Jewelry.service.VoucherService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping({"/admin/vouchers", "/staff/vouchers"})
public class VoucherController {

    private static final DateTimeFormatter FORM_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    public String list(Model model, HttpServletRequest request) {
        String basePath = resolveBasePath(request);
        model.addAttribute("vouchers", voucherService.findAll());
        model.addAttribute("activePage", isStaffPath(basePath) ? "staff-vouchers" : "vouchers");
        model.addAttribute("basePath", basePath);
        return "admin/vouchers";
    }

    @GetMapping("/new")
    public String createForm(Model model, HttpServletRequest request) {
        String basePath = resolveBasePath(request);
        model.addAttribute("isEdit", false);
        model.addAttribute("activePage", isStaffPath(basePath) ? "staff-vouchers" : "vouchers");
        model.addAttribute("basePath", basePath);
        return "admin/voucher-form";
    }

    @PostMapping
    public String create(@RequestParam String code,
                         @RequestParam BigDecimal discountValue,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        String basePath = resolveBasePath(request);
        try {
            LocalDateTime start = parseDateTime(startDate);
            LocalDateTime end = parseDateTime(endDate);
            if (isInvalidRange(start, end)) {
                redirectAttributes.addFlashAttribute("error", "Thời gian kết thúc phải sau hoặc bằng thời gian bắt đầu.");
                return "redirect:" + basePath + "/new";
            }

            Voucher voucher = new Voucher();
            voucher.setCode(code != null ? code.trim() : null);
            voucher.setDiscountValue(discountValue);
            voucher.setDescription(normalize(description));
            voucher.setStartDate(start);
            voucher.setEndDate(end);
            voucherService.save(voucher);
            redirectAttributes.addFlashAttribute("success", "Thêm voucher thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi thêm voucher: " + e.getMessage());
            return "redirect:" + basePath + "/new";
        }
        return "redirect:" + basePath;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id,
                           Model model,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
        String basePath = resolveBasePath(request);
        return voucherService.findById(id).map(voucher -> {
            model.addAttribute("voucher", voucher);
            model.addAttribute("isEdit", true);
            model.addAttribute("activePage", isStaffPath(basePath) ? "staff-vouchers" : "vouchers");
            model.addAttribute("basePath", basePath);
            model.addAttribute("startDateValue", formatDateTime(voucher.getStartDate()));
            model.addAttribute("endDateValue", formatDateTime(voucher.getEndDate()));
            return "admin/voucher-form";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy voucher.");
            return "redirect:" + basePath;
        });
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id,
                         @RequestParam String code,
                         @RequestParam BigDecimal discountValue,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        String basePath = resolveBasePath(request);
        try {
            Voucher voucher = voucherService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy voucher."));
            LocalDateTime start = parseDateTime(startDate);
            LocalDateTime end = parseDateTime(endDate);
            if (isInvalidRange(start, end)) {
                redirectAttributes.addFlashAttribute("error", "Thời gian kết thúc phải sau hoặc bằng thời gian bắt đầu.");
                return "redirect:" + basePath + "/" + id + "/edit";
            }

            voucher.setCode(code != null ? code.trim() : null);
            voucher.setDiscountValue(discountValue);
            voucher.setDescription(normalize(description));
            voucher.setStartDate(start);
            voucher.setEndDate(end);
            voucherService.save(voucher);
            redirectAttributes.addFlashAttribute("success", "Cập nhật voucher thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật voucher: " + e.getMessage());
            return "redirect:" + basePath + "/" + id + "/edit";
        }
        return "redirect:" + basePath;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id,
                         RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        String basePath = resolveBasePath(request);
        try {
            voucherService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Xóa voucher thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa voucher: " + e.getMessage());
        }
        return "redirect:" + basePath;
    }

    private String resolveBasePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/staff/vouchers")) {
            return "/staff/vouchers";
        }
        return "/admin/vouchers";
    }

    private boolean isStaffPath(String basePath) {
        return basePath.startsWith("/staff");
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, FORM_DATE_TIME);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Định dạng thời gian không hợp lệ.");
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(FORM_DATE_TIME);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isInvalidRange(LocalDateTime startDate, LocalDateTime endDate) {
        return startDate != null && endDate != null && endDate.isBefore(startDate);
    }
}
