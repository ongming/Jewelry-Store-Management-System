package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.OrderDetail;
import com.example.Jewelry.repository.OrderDetailRepository;
import com.example.Jewelry.service.OrderDetailService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderDetailServiceImpl implements OrderDetailService {

    private final OrderDetailRepository OrderDetailRepository;

    public OrderDetailServiceImpl(OrderDetailRepository OrderDetailRepository) {
        this.OrderDetailRepository = OrderDetailRepository;
    }

    @Override
    public Optional<OrderDetail> findById(Integer id) {
        return OrderDetailRepository.findById(id);
    }

    @Override
    public List<OrderDetail> findAll() {
        return OrderDetailRepository.findAll();
    }

    @Override
    public OrderDetail save(OrderDetail entity) {
        return OrderDetailRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        OrderDetailRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return OrderDetailRepository.existsById(id);
    }

    @Override
    public long count() {
        return OrderDetailRepository.count();
    }
}
