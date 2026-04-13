package com.example.Jewelry.model.state;

import com.example.Jewelry.model.entity.Account;

/**
 * Trạng thái LOCKED - Tài khoản bị khóa (do sai mật khẩu nhiều lần)
 */
public class LockedOutState implements AccountState {
    
    @Override
    public boolean canLogin() {
        // Tài khoản LOCKED KHÔNG thể login
        return false;
    }
    
    @Override
    public boolean canAccessSystem() {
        return false;
    }
    
    @Override
    public boolean canModifyData() {
        return false;
    }
    
    @Override
    public void suspend(Account account) {
        // Tài khoản đã locked, không thể suspend thêm
        System.out.println("⚠️ Tài khoản đã ở trạng thái LOCKED");
    }
    
    @Override
    public void activate(Account account) {
        // Chuyển từ LOCKED → ACTIVE (cần xác nhận từ admin)
        account.setState(new ActiveState());
        account.setStatus("ACTIVE");
        System.out.println("✅ Tài khoản đã được mở khóa (ACTIVE)");
    }
    
    @Override
    public void lock(Account account) {
        // Đã ở LOCKED
        System.out.println("⚠️ Tài khoản đã ở trạng thái LOCKED");
    }
    
    @Override
    public String getStateName() {
        return "LOCKED";
    }
}
