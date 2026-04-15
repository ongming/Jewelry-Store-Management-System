package com.example.Jewelry.repository;

import com.example.Jewelry.model.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

	boolean existsByProduct_ProductId(Integer productId);

	@Query("""
		select od
		from OrderDetail od
		where od.order.status = :status
		  and od.order.orderDate between :fromTime and :toTime
		""")
	List<OrderDetail> findByOrderStatusAndOrderDateRange(@Param("status") String status,
														 @Param("fromTime") LocalDateTime from,
														 @Param("toTime") LocalDateTime to);
}
