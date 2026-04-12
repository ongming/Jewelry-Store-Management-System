package com.example.Jewelry.service.impl;

import com.example.Jewelry.model.entity.Customer;
import com.example.Jewelry.repository.CustomerRepository;
import com.example.Jewelry.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository CustomerRepository;

    public CustomerServiceImpl(CustomerRepository CustomerRepository) {
        this.CustomerRepository = CustomerRepository;
    }

    @Override
    public Optional<Customer> findById(Integer id) {
        return CustomerRepository.findById(id);
    }

    @Override
    public List<Customer> findAll() {
        return CustomerRepository.findAllByOrderByCustomerNameAsc();
    }

    @Override
    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String normalized = keyword.trim();
        return CustomerRepository.findByCustomerNameContainingIgnoreCaseOrPhoneContainingIgnoreCaseOrderByCustomerNameAsc(
            normalized,
            normalized
        );
    }

    @Override
    public Customer save(Customer entity) {
        return CustomerRepository.save(entity);
    }

    @Override
    public void deleteById(Integer id) {
        CustomerRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return CustomerRepository.existsById(id);
    }

    @Override
    public long count() {
        return CustomerRepository.count();
    }
}
