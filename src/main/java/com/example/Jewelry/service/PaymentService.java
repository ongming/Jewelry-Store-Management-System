package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentService {

    Optional<Payment> findById(Integer id);

    List<Payment> findAll();

    Payment save(Payment entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
