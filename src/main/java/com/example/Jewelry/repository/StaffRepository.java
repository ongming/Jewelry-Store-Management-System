package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StaffRepository extends JpaRepository<Staff, Integer> {

    @Query("select coalesce(max(s.staffId), 1000) from Staff s")
    int findMaxStaffId();
}
