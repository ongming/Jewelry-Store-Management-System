package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Voucher;
import com.example.Jewelry.repository.VoucherRepository;
import com.example.Jewelry.service.VoucherService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository VoucherRepository;

    public VoucherServiceImpl(VoucherRepository VoucherRepository) {
        this.VoucherRepository = VoucherRepository;
    }

    @Override
    public Optional<Voucher> findById(Integer id) {
        return VoucherRepository.findById(id);
    }

    @Override
    public Optional<Voucher> findByCode(String code) {
        return VoucherRepository.findByCodeIgnoreCase(code);
    }

    @Override
    public List<Voucher> findAll() {
        return VoucherRepository.findAll();
    }

    @Override
    public Voucher save(Voucher entity) {
        return VoucherRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        VoucherRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return VoucherRepository.existsById(id);
    }

    @Override
    public long count() {
        return VoucherRepository.count();
    }
}
