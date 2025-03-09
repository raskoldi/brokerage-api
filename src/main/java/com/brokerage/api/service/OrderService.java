package com.brokerage.api.service;
import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.dto.request.OrderFilterRequest;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.api.model.Order;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    List<OrderResponse> getOrdersByCustomerIdAndDateRange(Long customerId, LocalDateTime startDate, LocalDateTime endDate);

    List<OrderResponse> filterOrders(OrderFilterRequest request);

    OrderResponse cancelOrder(Long orderId, Long customerId);

    // Bonus 2 - Match pending orders
    OrderResponse matchOrder(Long orderId);
}
