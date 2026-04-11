package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.OrderDetail;

import java.util.List;
import java.util.Optional;

public interface OrderDetailService {

    Optional<OrderDetail> findById(Integer id);

    List<OrderDetail> findAll();

    OrderDetail save(OrderDetail entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
