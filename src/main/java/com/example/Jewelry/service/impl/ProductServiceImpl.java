package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Product;
import com.example.Jewelry.repository.ProductRepository;
import com.example.Jewelry.service.ProductService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository ProductRepository;

    public ProductServiceImpl(ProductRepository ProductRepository) {
        this.ProductRepository = ProductRepository;
    }

    @Override
    public Optional<Product> findById(Integer id) {
        return ProductRepository.findById(id);
    }

    @Override
    public List<Product> findAll() {
        return ProductRepository.findAll();
    }

    @Override
    public Product save(Product entity) {
        return ProductRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        ProductRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return ProductRepository.existsById(id);
    }

    @Override
    public long count() {
        return ProductRepository.count();
    }
}
