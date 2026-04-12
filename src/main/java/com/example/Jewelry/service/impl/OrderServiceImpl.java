package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Order;
import com.example.Jewelry.repository.OrderRepository;
import com.example.Jewelry.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository OrderRepository;

    public OrderServiceImpl(OrderRepository OrderRepository) {
        this.OrderRepository = OrderRepository;
    }

    @Override
    public Optional<Order> findById(Integer id) {
        return OrderRepository.findById(id);
    }

    @Override
    public List<Order> findAll() {
        return OrderRepository.findAll();
    }

    @Override
    public List<Order> findByCustomerId(Integer customerId) {
        return OrderRepository.findByCustomer_CustomerIdOrderByOrderDateDesc(customerId);
    }

    @Override
    public boolean existsByCustomerId(Integer customerId) {
        return OrderRepository.existsByCustomer_CustomerId(customerId);
    }

    @Override
    public Order save(Order entity) {
        return OrderRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        OrderRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return OrderRepository.existsById(id);
    }

    @Override
    public long count() {
        return OrderRepository.count();
    }
}
