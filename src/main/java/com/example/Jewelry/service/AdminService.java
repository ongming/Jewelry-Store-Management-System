package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Admin;

import java.util.List;
import java.util.Optional;

public interface AdminService {

    Optional<Admin> findById(Integer id);

    List<Admin> findAll();

    Admin save(Admin entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
