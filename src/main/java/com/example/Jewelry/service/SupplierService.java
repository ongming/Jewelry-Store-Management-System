package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierService {

    Optional<Supplier> findById(Integer id);

    List<Supplier> findAll();

    Supplier createSupplier(String supplierName, String phone, String address);

    Supplier updateSupplier(Integer id, String supplierName, String phone, String address);

    void deleteSupplier(Integer id);

    Supplier save(Supplier entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}