package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.ImportDetail;
import com.example.Jewelry.repository.ImportDetailRepository;
import com.example.Jewelry.service.ImportDetailService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ImportDetailServiceImpl implements ImportDetailService {

    private final ImportDetailRepository ImportDetailRepository;

    public ImportDetailServiceImpl(ImportDetailRepository ImportDetailRepository) {
        this.ImportDetailRepository = ImportDetailRepository;
    }

    @Override
    public Optional<ImportDetail> findById(Integer id) {
        return ImportDetailRepository.findById(id);
    }

    @Override
    public List<ImportDetail> findAll() {
        return ImportDetailRepository.findAll();
    }

    @Override
    public ImportDetail save(ImportDetail entity) {
        return ImportDetailRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        ImportDetailRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return ImportDetailRepository.existsById(id);
    }

    @Override
    public long count() {
        return ImportDetailRepository.count();
    }
}
