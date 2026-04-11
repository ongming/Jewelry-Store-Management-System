package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Staff;

import java.util.List;
import java.util.Optional;

public interface StaffService {

    Optional<Staff> findById(Integer id);

    List<Staff> findAll();

    Staff save(Staff entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
