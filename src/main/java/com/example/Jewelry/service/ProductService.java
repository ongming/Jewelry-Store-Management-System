package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    Optional<Product> findById(Integer id);

    List<Product> findAll();

    Product save(Product entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
