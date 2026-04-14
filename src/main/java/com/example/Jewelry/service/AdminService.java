package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.model.entity.Admin;
import com.example.Jewelry.model.entity.Staff;

import java.util.List;
import java.util.Optional;

public interface AdminService {

    Optional<Admin> findById(Integer id);

    List<Admin> findAll();

    Admin save(Admin entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();

    /**
     * Lấy danh sách Staff mà Admin quản lý (ACTIVE) + tất cả Staff INACTIVE
     * Admin sẽ thấy Staff của mình và có thể kích hoạt bất kỳ Staff INACTIVE nào
     */
    List<Staff> getVisibleStaff(Integer adminAccountId);

    /**
     * Kích hoạt Staff INACTIVE và gán Manager Admin = admin hiện tại
     * @param staffAccountId ID của Staff cần kích hoạt
     * @param adminAccountId ID của Admin thực hiện kích hoạt
     */
    void activateInactiveStaff(Integer staffAccountId, Integer adminAccountId);

    /**
     * Lấy tất cả tài khoản INACTIVE mà Admin có thể thấy
     * Bao gồm: tất cả Staff INACTIVE + tất cả Admin INACTIVE
     */
    List<Account> getAllVisibleInactiveAccounts();
}
