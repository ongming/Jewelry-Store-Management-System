package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Voucher;
import com.example.Jewelry.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherApiController {

    private final VoucherService voucherService;

    public VoucherApiController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkVoucher(@RequestParam String code) {
        try {
            if (code == null || code.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Mã voucher không được để trống");
                System.out.println("DEBUG: Voucher code is empty");
                return ResponseEntity.ok(response);
            }

            String trimmedCode = code.trim();
            System.out.println("DEBUG: Checking voucher with code: " + trimmedCode);
            
            Voucher voucher = voucherService.findByCode(trimmedCode).orElse(null);
            
            System.out.println("DEBUG: Voucher found: " + (voucher != null ? "YES" : "NO"));
            if (voucher != null) {
                System.out.println("DEBUG: Voucher ID: " + voucher.getVoucherId() + ", Code: " + voucher.getCode() + ", Discount: " + voucher.getDiscountValue());
            }
            
            Map<String, Object> response = new HashMap<>();
            
            if (voucher == null) {
                response.put("success", false);
                response.put("voucherId", null);
                response.put("message", "Mã voucher không tồn tại hoặc không hợp lệ");
                return ResponseEntity.ok(response);
            }

            if (!isVoucherActive(voucher)) {
                response.put("success", false);
                response.put("voucherId", null);
                response.put("message", "Voucher chưa đến thời gian áp dụng hoặc đã hết hạn");
                return ResponseEntity.ok(response);
            }
            
            // Voucher hợp lệ
            response.put("success", true);
            response.put("voucherId", voucher.getVoucherId());
            response.put("code", voucher.getCode());
            response.put("discountValue", voucher.getDiscountValue() != null ? voucher.getDiscountValue().doubleValue() : 0);
            response.put("description", voucher.getDescription());
            response.put("startDate", voucher.getStartDate() != null ? voucher.getStartDate().toString() : null);
            response.put("endDate", voucher.getEndDate() != null ? voucher.getEndDate().toString() : null);
            response.put("message", "Voucher hợp lệ");
            
            System.out.println("DEBUG: Returning success response with discount: " + response.get("discountValue"));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("DEBUG: Exception occurred: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("voucherId", null);
            response.put("message", "Lỗi server: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> availableVouchers() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> vouchers = voucherService.findAll().stream()
                .filter(voucher -> voucher.getCode() != null && !voucher.getCode().isBlank() && isVoucherActive(voucher))
                .map(voucher -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("voucherId", voucher.getVoucherId());
                    item.put("code", voucher.getCode());
                    item.put("discountValue", voucher.getDiscountValue() != null ? voucher.getDiscountValue().doubleValue() : 0);
                    item.put("description", voucher.getDescription());
                    item.put("startDate", voucher.getStartDate() != null ? voucher.getStartDate().toString() : null);
                    item.put("endDate", voucher.getEndDate() != null ? voucher.getEndDate().toString() : null);
                    return item;
                })
                .toList();

            response.put("success", true);
            response.put("vouchers", vouchers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("vouchers", List.of());
            response.put("message", "Không thể tải danh sách voucher.");
            return ResponseEntity.ok(response);
        }
    }

    private boolean isVoucherActive(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = voucher.getStartDate();
        LocalDateTime endDate = voucher.getEndDate();

        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }
        return true;
    }
}
