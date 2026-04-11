package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Admin;
import com.example.Jewelry.repository.AdminRepository;
import com.example.Jewelry.service.AdminService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminRepository AdminRepository;

    public AdminServiceImpl(AdminRepository AdminRepository) {
        this.AdminRepository = AdminRepository;
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
}
