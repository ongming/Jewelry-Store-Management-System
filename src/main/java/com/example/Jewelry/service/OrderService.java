package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    Optional<Order> findById(Integer id);

    List<Order> findAll();

    List<Order> findByCustomerId(Integer customerId);

    boolean existsByCustomerId(Integer customerId);

    Order save(Order entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
