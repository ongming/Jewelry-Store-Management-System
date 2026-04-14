package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Integer> {

    @Query("select coalesce(max(s.staffId), 1000) from Staff s")
    int findMaxStaffId();

    @Query("""
        select s
        from Staff s
        join fetch s.managerAdmin m
        where upper(s.roleName) = 'STAFF'
          and m.accountId = :managerAdminAccountId
    """)
    List<Staff> findManagedStaffWithManager(@Param("managerAdminAccountId") Integer managerAdminAccountId);

    @Query("""
        select s
        from Staff s
        join fetch s.managerAdmin m
        where upper(s.roleName) = 'STAFF'
          and s.accountId = :accountId
          and m.accountId = :managerAdminAccountId
    """)
    Optional<Staff> findManagedStaffByAccountId(@Param("accountId") Integer accountId,
                                                @Param("managerAdminAccountId") Integer managerAdminAccountId);

    /**
     * Lấy danh sách Staff INACTIVE (chưa kích hoạt)
     */
    @Query("""
        select s
        from Staff s
        where upper(s.roleName) = 'STAFF'
          and upper(s.status) = 'INACTIVE'
    """)
    List<Staff> findAllInactiveStaff();

    /**
     * Lấy danh sách Staff được quản lý bởi admin (ACTIVE) + tất cả Staff INACTIVE
     * Để admin có thể xem Staff của mình + các Staff chưa được kích hoạt
     */
    @Query("""
        select s
        from Staff s
        left join fetch s.managerAdmin m
        where upper(s.roleName) = 'STAFF'
          and (
            (m.accountId = :managerAdminAccountId and upper(s.status) = 'ACTIVE')
            or upper(s.status) = 'INACTIVE'
          )
        order by s.status desc, s.fullName asc
    """)
    List<Staff> findManagedAndInactiveStaff(@Param("managerAdminAccountId") Integer managerAdminAccountId);
}
