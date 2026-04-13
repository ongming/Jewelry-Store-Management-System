package com.example.Jewelry.factory;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.model.entity.Admin;
import com.example.Jewelry.model.entity.Staff;

/**
 * Factory class để khởi tạo Account dựa trên Role theo mẫu Factory Pattern.
 */
public class AccountFactory {

    /**
     * Tạo đối tượng Account (Admin hoặc Staff) dựa trên role được truyền vào.
     * @param role Vai trò ("ADMIN" hoặc "STAFF")
     * @return Đối tượng Account tương ứng.
     */
    public static Account createAccount(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        
        if (role.equalsIgnoreCase("ADMIN")) {
            Admin admin = new Admin();
            admin.setRoleName("ADMIN");
            return admin;
        } else if (role.equalsIgnoreCase("STAFF")) {
            Staff staff = new Staff();
            staff.setRoleName("STAFF");
            return staff;
        }
        
        throw new IllegalArgumentException("Unknown role type: " + role);
    }
}
