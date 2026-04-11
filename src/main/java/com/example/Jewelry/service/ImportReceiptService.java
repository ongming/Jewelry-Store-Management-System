package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.ImportReceipt;

import java.util.List;
import java.util.Optional;

public interface ImportReceiptService {

    Optional<ImportReceipt> findById(Integer id);

    List<ImportReceipt> findAll();

    ImportReceipt save(ImportReceipt entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
