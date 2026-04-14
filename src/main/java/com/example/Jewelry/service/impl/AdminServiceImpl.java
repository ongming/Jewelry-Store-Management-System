package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Account;
import com.example.Jewelry.model.entity.Admin;
import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.repository.AccountRepository;
import com.example.Jewelry.repository.AdminRepository;
import com.example.Jewelry.repository.StaffRepository;
import com.example.Jewelry.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminRepository AdminRepository;
    private final StaffRepository staffRepository;
    private final AccountRepository accountRepository;

    public AdminServiceImpl(AdminRepository AdminRepository, StaffRepository staffRepository, AccountRepository accountRepository) {
        this.AdminRepository = AdminRepository;
        this.staffRepository = staffRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public Optional<Admin> findById(Integer id) {
        return AdminRepository.findById(id);
    }

    @Override
    public List<Admin> findAll() {
        return AdminRepository.findAll();
    }

    @Override
    public Admin save(Admin entity) {
        return AdminRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        AdminRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return AdminRepository.existsById(id);
    }

    @Override
    public long count() {
        return AdminRepository.count();
    }

    @Override
    public List<Staff> getVisibleStaff(Integer adminAccountId) {
        // Lấy danh sách Staff được quản lý + tất cả Staff INACTIVE
        return staffRepository.findManagedAndInactiveStaff(adminAccountId);
    }

    @Override
    @Transactional
    public void activateInactiveStaff(Integer staffAccountId, Integer adminAccountId) {
        // Kiểm tra Staff tồn tại
        Optional<Staff> staffOptional = staffRepository.findById(staffAccountId);
        if (staffOptional.isEmpty()) {
            throw new RuntimeException("Staff không tồn tại");
        }

        Staff staff = staffOptional.get();

        // Kiểm tra Staff có phải INACTIVE không
        if (!"INACTIVE".equalsIgnoreCase(staff.getStatus())) {
            throw new RuntimeException("Staff không ở trạng thái INACTIVE");
        }

        // Lấy Admin kích hoạt
        Optional<Admin> adminOptional = AdminRepository.findById(adminAccountId);
        if (adminOptional.isEmpty()) {
            throw new RuntimeException("Admin không tồn tại");
        }

        Admin manager = adminOptional.get();

        // Cập nhật Staff:
        // 1. Đổi status thành ACTIVE
        staff.setStatus("ACTIVE");
        // 2. Gán Manager Admin = admin vừa kích hoạt
        staff.setManagerAdmin(manager);

        // Lưu lại
        staffRepository.save(staff);
    }

    @Override
    public List<Account> getAllVisibleInactiveAccounts() {
        // Trả về tất cả tài khoản INACTIVE (Staff hay Admin đều được)
        return accountRepository.findByStatusIgnoreCase("INACTIVE");
    }
}
