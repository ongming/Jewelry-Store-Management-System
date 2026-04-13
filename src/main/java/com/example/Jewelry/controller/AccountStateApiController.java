package com.example.Jewelry.controller;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API Controller cho việc quản lý trạng thái tài khoản (Account State)
 * Sử dụng State Pattern để quản lý các trạng thái: ACTIVE, SUSPENDED, LOCKED
 */
@Controller
@RequestMapping("/api/accounts")
public class AccountStateApiController {

    private final AccountService accountService;

    public AccountStateApiController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Tạm khóa tài khoản (ACTIVE → SUSPENDED)
     */
    @PostMapping("/{accountId}/suspend")
    @ResponseBody
    public ResponseEntity<?> suspendAccount(@PathVariable Integer accountId) {
        try {
            accountService.suspendAccount(accountId);
            
            Account account = accountService.findById(accountId).orElse(null);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tài khoản đã được tạm khóa thành công");
            response.put("accountId", accountId);
            response.put("status", account != null ? account.getStatus() : "SUSPENDED");
            response.put("stateName", account != null ? account.getAccountStateName() : "SUSPENDED");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Kích hoạt lại tài khoản (SUSPENDED/LOCKED → ACTIVE)
     */
    @PostMapping("/{accountId}/activate")
    @ResponseBody
    public ResponseEntity<?> activateAccount(@PathVariable Integer accountId) {
        try {
            accountService.activateAccount(accountId);
            
            Account account = accountService.findById(accountId).orElse(null);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tài khoản đã được kích hoạt thành công");
            response.put("accountId", accountId);
            response.put("status", account != null ? account.getStatus() : "ACTIVE");
            response.put("stateName", account != null ? account.getAccountStateName() : "ACTIVE");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Khóa tài khoản (do sai mật khẩu nhiều lần)
     */
    @PostMapping("/{accountId}/lock")
    @ResponseBody
    public ResponseEntity<?> lockAccount(@PathVariable Integer accountId) {
        try {
            accountService.lockAccount(accountId);
            
            Account account = accountService.findById(accountId).orElse(null);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tài khoản đã được khóa");
            response.put("accountId", accountId);
            response.put("status", account != null ? account.getStatus() : "LOCKED");
            response.put("stateName", account != null ? account.getAccountStateName() : "LOCKED");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Lấy thông tin trạng thái tài khoản
     */
    @PostMapping("/{accountId}/state")
    @ResponseBody
    public ResponseEntity<?> getAccountState(@PathVariable Integer accountId) {
        try {
            Account account = accountService.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accountId", accountId);
            response.put("username", account.getUsername());
            response.put("fullName", account.getFullName());
            response.put("status", account.getStatus());
            response.put("stateName", account.getAccountStateName());
            response.put("canLogin", account.canLogin());
            response.put("canAccessSystem", account.canAccessSystem());
            response.put("canModifyData", account.canModifyData());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Lấy danh sách tất cả accounts với thông tin state
     */
    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<?> getAllAccounts() {
        try {
            List<Account> accounts = accountService.findAll();
            
            List<Map<String, Object>> accountsData = accounts.stream()
                .map(account -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("accountId", account.getAccountId());
                    data.put("username", account.getUsername());
                    data.put("fullName", account.getFullName());
                    data.put("roleName", account.getRoleName());
                    data.put("status", account.getStatus());
                    data.put("stateName", account.getAccountStateName());
                    data.put("canLogin", account.canLogin());
                    data.put("canAccessSystem", account.canAccessSystem());
                    data.put("canModifyData", account.canModifyData());
                    return data;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", accountsData);
            response.put("total", accountsData.size());
            
            return ResponseEntity.ok(accountsData);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * Lấy danh sách accounts theo trạng thái cụ thể
     */
    @GetMapping("/by-status/{status}")
    @ResponseBody
    public ResponseEntity<?> getAccountsByStatus(@PathVariable String status) {
        try {
            List<Account> accounts = accountService.findByStatus(status);
            
            List<Map<String, Object>> accountsData = accounts.stream()
                .map(account -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("accountId", account.getAccountId());
                    data.put("username", account.getUsername());
                    data.put("fullName", account.getFullName());
                    data.put("roleName", account.getRoleName());
                    data.put("status", account.getStatus());
                    data.put("stateName", account.getAccountStateName());
                    return data;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(accountsData);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}