package com.example.Jewelry.model.state;

import com.example.Jewelry.model.entity.Account;

/**
 * Trạng thái SUSPENDED - Tài khoản bị tạm khóa
 */
public class SuspendedState implements AccountState {
    
    @Override
    public boolean canLogin() {
        // Tài khoản SUSPENDED KHÔNG thể login
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
        // Đã ở SUSPENDED, không cần làm gì
        System.out.println("⚠️ Tài khoản đã ở trạng thái SUSPENDED");
    }
    
    @Override
    public void activate(Account account) {
        // Chuyển từ SUSPENDED → ACTIVE
        account.setState(new ActiveState());
        account.setStatus("ACTIVE");
        System.out.println("✅ Tài khoản đã được kích hoạt lại (ACTIVE)");
    }
    
    @Override
    public void lock(Account account) {
        // Chuyển từ SUSPENDED → LOCKED
        account.setState(new LockedOutState());
        account.setStatus("LOCKED");
        System.out.println("✅ Tài khoản đã bị khóa (LOCKED)");
    }
    
    @Override
    public String getStateName() {
        return "SUSPENDED";
    }
}
