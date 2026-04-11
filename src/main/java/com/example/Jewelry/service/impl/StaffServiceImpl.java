package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Staff;
import com.example.Jewelry.repository.StaffRepository;
import com.example.Jewelry.service.StaffService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StaffServiceImpl implements StaffService {

    private final StaffRepository StaffRepository;

    public StaffServiceImpl(StaffRepository StaffRepository) {
        this.StaffRepository = StaffRepository;
    }

    @Override
    public Optional<Staff> findById(Integer id) {
        return StaffRepository.findById(id);
    }

    @Override
    public List<Staff> findAll() {
        return StaffRepository.findAll();
    }

    @Override
    public Staff save(Staff entity) {
        return StaffRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        StaffRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return StaffRepository.existsById(id);
    }

    @Override
    public long count() {
        return StaffRepository.count();
    }
}
