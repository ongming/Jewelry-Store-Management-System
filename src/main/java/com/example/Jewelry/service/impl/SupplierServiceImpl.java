package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Supplier;
import com.example.Jewelry.repository.SupplierRepository;
import com.example.Jewelry.service.SupplierService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository SupplierRepository;

    public SupplierServiceImpl(SupplierRepository SupplierRepository) {
        this.SupplierRepository = SupplierRepository;
    }

    @Override
    public Supplier createSupplier(String supplierName, String phone, String address) {
        Supplier supplier = new Supplier();
        supplier.setSupplierName(supplierName);
        supplier.setPhone(phone);
        supplier.setAddress(address);
        return SupplierRepository.save(supplier);
    }

    @Override
    public Supplier updateSupplier(Integer id, String supplierName, String phone, String address) {
        Supplier supplier = SupplierRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Nhà cung cấp không tồn tại"));
        supplier.setSupplierName(supplierName);
        supplier.setPhone(phone);
        supplier.setAddress(address);
        return SupplierRepository.save(supplier);
    }

    @Override
    public void deleteSupplier(Integer id) {
        if (!SupplierRepository.existsById(id)) {
            throw new IllegalArgumentException("Nhà cung cấp không tồn tại");
        }
        SupplierRepository.deleteById(id);
    }

    @Override
    public Optional<Supplier> findById(Integer id) {
        return SupplierRepository.findById(id);
    }

    @Override
    public List<Supplier> findAll() {
        return SupplierRepository.findAll();
    }

    @Override
    public Supplier save(Supplier entity) {
        return SupplierRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        SupplierRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return SupplierRepository.existsById(id);
    }

    @Override
    public long count() {
        return SupplierRepository.count();
    }
}