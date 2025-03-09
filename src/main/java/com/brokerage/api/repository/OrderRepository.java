package com.brokerage.api.repository;

import com.brokerage.api.model.Order;
import com.brokerage.api.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId AND o.createDate BETWEEN :startDate AND :endDate")
    List<Order> findByCustomerIdAndDateRange(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    Optional<Order> findByIdAndStatus(Long id, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);
}