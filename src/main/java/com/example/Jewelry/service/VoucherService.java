package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Voucher;

import java.util.List;
import java.util.Optional;

public interface VoucherService {

    Optional<Voucher> findById(Integer id);

    Optional<Voucher> findByCode(String code);

    List<Voucher> findAll();

    Voucher save(Voucher entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
