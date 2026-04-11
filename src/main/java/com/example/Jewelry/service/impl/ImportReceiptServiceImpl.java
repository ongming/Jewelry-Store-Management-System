package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.ImportReceipt;
import com.example.Jewelry.repository.ImportReceiptRepository;
import com.example.Jewelry.service.ImportReceiptService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ImportReceiptServiceImpl implements ImportReceiptService {

    private final ImportReceiptRepository ImportReceiptRepository;

    public ImportReceiptServiceImpl(ImportReceiptRepository ImportReceiptRepository) {
        this.ImportReceiptRepository = ImportReceiptRepository;
    }

    @Override
    public Optional<ImportReceipt> findById(Integer id) {
        return ImportReceiptRepository.findById(id);
    }

    @Override
    public List<ImportReceipt> findAll() {
        return ImportReceiptRepository.findAll();
    }

    @Override
    public ImportReceipt save(ImportReceipt entity) {
        return ImportReceiptRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        ImportReceiptRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return ImportReceiptRepository.existsById(id);
    }

    @Override
    public long count() {
        return ImportReceiptRepository.count();
    }
}
