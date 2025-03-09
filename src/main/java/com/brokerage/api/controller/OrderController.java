package com.brokerage.api.controller;

import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.dto.request.OrderFilterRequest;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.api.model.Customer;
import com.brokerage.api.model.User;
import com.brokerage.api.repository.CustomerRepository;
import com.brokerage.api.repository.UserRepository;
import com.brokerage.api.security.CurrentUser;
import com.brokerage.api.security.UserPrincipal;
import com.brokerage.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request,
                                                     @CurrentUser UserPrincipal currentUser) {
        log.info("Create order request received: {}", request);
        // If admin, allow creating orders for any customer
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            log.info("Admin user creating order for customer ID: {}", request.getCustomerId());
            return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
        }

        // If regular user, verify they are creating an order for their own customer
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("No customer associated with this user"));

        // Verifying the customer ID in the request matches the user's customer ID
        if (!customer.getId().equals(request.getCustomerId())) {
            log.warn("User {} attempted to create order for customer ID {} but is associated with customer ID {}",
                    user.getUsername(), request.getCustomerId(), customer.getId());
            throw new AccessDeniedException("You can only create orders for your own account");
        }

        log.info("Customer user creating order for their account (ID: {})", customer.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCustomerOwner(#customerId, authentication)")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerIdAndDateRange(
            @RequestParam Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (startDate == null) {
            startDate = LocalDateTime.now().minusYears(10);
        }

        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        return ResponseEntity.ok(orderService.getOrdersByCustomerIdAndDateRange(customerId, startDate, endDate));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCustomerOwner(#request.customerId, authentication)")
    public ResponseEntity<List<OrderResponse>> filterOrders(@RequestBody OrderFilterRequest request) {
        return ResponseEntity.ok(orderService.filterOrders(request));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isOrderOwner(#orderId, authentication)")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @CurrentUser UserPrincipal currentUser) {

        Long customerId = null;

        // If admin is canceling, need to get the customerId from the order
        if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.info("Admin cancelling order ID: {}", orderId);
            customerId = null; // Service will handle getting customer ID from order
        } else {
            User user = userRepository.findById(currentUser.getId()).orElseThrow();
            Customer customer = customerRepository.findByUser(user).orElseThrow();
            customerId = customer.getId();
            log.info("Customer user (ID: {}) cancelling order ID: {}", customerId, orderId);
        }

        return ResponseEntity.ok(orderService.cancelOrder(orderId, customerId));
    }
}