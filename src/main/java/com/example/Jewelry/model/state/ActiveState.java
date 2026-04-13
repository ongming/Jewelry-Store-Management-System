package com.example.Jewelry.model.state;

import com.example.Jewelry.model.entity.Account;

/**
 * Trạng thái ACTIVE - Tài khoản đang hoạt động bình thường
 */
public class ActiveState implements AccountState {
    
    @Override
    public boolean canLogin() {
        // Tài khoản ACTIVE có thể login
        return true;
    }
    
    @Override
    public boolean canAccessSystem() {
        return true;
    }
    
    @Override
    public boolean canModifyData() {
        return true;
    }
    
    @Override
    public void suspend(Account account) {
        // Chuyển từ ACTIVE → SUSPENDED
        account.setState(new SuspendedState());
        account.setStatus("SUSPENDED");
        System.out.println("✅ Tài khoản đã bị tạm khóa (SUSPENDED)");
    }
    
    @Override
    public void activate(Account account) {
        // Đã ở trạng thái ACTIVE, không cần làm gì
        System.out.println("⚠️ Tài khoản đã ở trạng thái ACTIVE");
    }
    
    @Override
    public void lock(Account account) {
        // Chuyển từ ACTIVE → LOCKED
        account.setState(new LockedOutState());
        account.setStatus("LOCKED");
        System.out.println("✅ Tài khoản đã bị khóa (LOCKED)");
    }
    
    @Override
    public String getStateName() {
        return "ACTIVE";
    }
}
