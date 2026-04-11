package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.ProductAttribute;
import com.example.Jewelry.repository.ProductAttributeRepository;
import com.example.Jewelry.service.ProductAttributeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductAttributeServiceImpl implements ProductAttributeService {

    private final ProductAttributeRepository ProductAttributeRepository;

    public ProductAttributeServiceImpl(ProductAttributeRepository ProductAttributeRepository) {
        this.ProductAttributeRepository = ProductAttributeRepository;
    }

    @Override
    public Optional<ProductAttribute> findById(Integer id) {
        return ProductAttributeRepository.findById(id);
    }

    @Override
    public List<ProductAttribute> findAll() {
        return ProductAttributeRepository.findAll();
    }

    @Override
    public ProductAttribute save(ProductAttribute entity) {
        return ProductAttributeRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        ProductAttributeRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return ProductAttributeRepository.existsById(id);
    }

    @Override
    public long count() {
        return ProductAttributeRepository.count();
    }
}
