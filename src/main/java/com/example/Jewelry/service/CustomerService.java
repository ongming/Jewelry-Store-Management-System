package com.example.Jewelry.service;

import com.example.Jewelry.model.entity.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerService {

    Optional<Customer> findById(Integer id);

    List<Customer> findAll();

    List<Customer> search(String keyword);

    Customer save(Customer entity);

    void deleteById(Integer id);

    boolean existsById(Integer id);

    long count();
}
