package com.example.Jewelry.model.state;

import com.example.Jewelry.model.entity.Account;

/**
 * Interface định nghĩa các hành vi của tài khoản theo từng trạng thái
 */
public interface AccountState {
    
    /**
     * Kiểm tra xem có thể login không
     */
    boolean canLogin();
    
    /**
     * Kiểm tra có thể truy cập hệ thống không
     */
    boolean canAccessSystem();
    
    /**
     * Kiểm tra có thể sửa đổi dữ liệu không
     */
    boolean canModifyData();
    
    /**
     * Chuyển sang trạng thái suspended
     */
    void suspend(Account account);
    
    /**
     * Chuyển sang trạng thái active
     */
    void activate(Account account);
    
    /**
     * Chuyển sang trạng thái locked
     */
    void lock(Account account);
    
    /**
     * Lấy tên trạng thái
     */
    String getStateName();
}
