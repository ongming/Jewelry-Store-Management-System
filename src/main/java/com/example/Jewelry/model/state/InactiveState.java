package com.example.Jewelry.model.state;

import com.example.Jewelry.model.entity.Account;

/**
 * Trạng thái INACTIVE - Tài khoản chưa được kích hoạt
 */
public class InactiveState implements AccountState {
    
    @Override
    public boolean canLogin() {
        // Tài khoản INACTIVE KHÔNG thể login
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
        // Tài khoản chưa active, không thể suspend
        System.out.println("⚠️ Tài khoản chưa được kích hoạt, không thể tạm khóa");
    }
    
    @Override
    public void activate(Account account) {
        // Chuyển từ INACTIVE → ACTIVE
        account.setState(new ActiveState());
        account.setStatus("ACTIVE");
        System.out.println("✅ Tài khoản đã được kích hoạt (ACTIVE)");
    }
    
    @Override
    public void lock(Account account) {
        // Tài khoản chưa active, không thể lock
        System.out.println("⚠️ Tài khoản chưa được kích hoạt, không thể khóa");
    }
    
    @Override
    public String getStateName() {
        return "INACTIVE";
    }
}
