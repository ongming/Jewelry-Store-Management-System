package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Payment;
import com.example.Jewelry.repository.PaymentRepository;
import com.example.Jewelry.service.PaymentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository PaymentRepository;

    public PaymentServiceImpl(PaymentRepository PaymentRepository) {
        this.PaymentRepository = PaymentRepository;
    }

    @Override
    public Optional<Payment> findById(Integer id) {
        return PaymentRepository.findById(id);
    }

    @Override
    public List<Payment> findAll() {
        return PaymentRepository.findAll();
    }

    @Override
    public Payment save(Payment entity) {
        return PaymentRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        PaymentRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return PaymentRepository.existsById(id);
    }

    @Override
    public long count() {
        return PaymentRepository.count();
    }
}
