package com.brokerage.api.controller;

import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.api.model.OrderSide;
import com.brokerage.api.model.OrderStatus;
import com.brokerage.api.model.User;
import com.brokerage.api.repository.CustomerRepository;
import com.brokerage.api.repository.UserRepository;
import com.brokerage.api.security.UserPrincipal;
import com.brokerage.api.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderController orderController;

    private UserPrincipal adminPrincipal;
    private UserPrincipal customerPrincipal;
    private User adminUser;
    private User customerUser;

    @BeforeEach
    public void setup() {
        // Creating admin principal
        adminPrincipal = new UserPrincipal(
                1L,
                "admin",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // Creating customer principal
        customerPrincipal = new UserPrincipal(
                2L,
                "customer1",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        // Creating admin user
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRoles(Collections.singletonList("ROLE_ADMIN"));

        // Create customer user
        customerUser = new User();
        customerUser.setId(2L);
        customerUser.setUsername("customer1");
        customerUser.setRoles(Collections.singletonList("ROLE_CUSTOMER"));
    }

    @Test
    public void createOrder_AsAdmin_Success() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(1L)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(10.0)
                .price(150.0)
                .build();

        OrderResponse expectedResponse = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(10.0)
                .price(150.0)
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(expectedResponse);

        // When
        ResponseEntity<OrderResponse> responseEntity = orderController.createOrder(request, adminPrincipal);

        // Then
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        OrderResponse actualResponse = responseEntity.getBody();
        assertNotNull(actualResponse);
        assertEquals(1L, actualResponse.getId());
        assertEquals(OrderStatus.PENDING, actualResponse.getStatus());

        verify(orderService, times(1)).createOrder(request);
    }

    @Test
    public void getOrdersByCustomerIdAndDateRange_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        OrderResponse order1 = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(10.0)
                .price(150.0)
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        OrderResponse order2 = OrderResponse.builder()
                .id(2L)
                .customerId(1L)
                .assetName("GOOGL")
                .orderSide(OrderSide.SELL)
                .size(5.0)
                .price(200.0)
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        List<OrderResponse> expectedOrders = Arrays.asList(order1, order2);

        when(orderService.getOrdersByCustomerIdAndDateRange(eq(1L), any(), any())).thenReturn(expectedOrders);

        // When
        ResponseEntity<List<OrderResponse>> responseEntity = orderController.getOrdersByCustomerIdAndDateRange(1L, startDate, endDate);

        // Then
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        List<OrderResponse> actualOrders = responseEntity.getBody();
        assertNotNull(actualOrders);
        assertEquals(2, actualOrders.size());
        assertEquals(1L, actualOrders.get(0).getId());
        assertEquals(2L, actualOrders.get(1).getId());

        verify(orderService, times(1)).getOrdersByCustomerIdAndDateRange(eq(1L), any(), any());
    }

    @Test
    public void cancelOrder_Success() {
        // Given
        OrderResponse expectedResponse = OrderResponse.builder()
                .id(1L)
                .customerId(1L)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(10.0)
                .price(150.0)
                .status(OrderStatus.CANCELED)
                .createDate(LocalDateTime.now())
                .build();

        when(orderService.cancelOrder(eq(1L), any())).thenReturn(expectedResponse);

        // When
        ResponseEntity<OrderResponse> responseEntity = orderController.cancelOrder(1L, adminPrincipal);

        // Then
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        OrderResponse actualResponse = responseEntity.getBody();
        assertNotNull(actualResponse);
        assertEquals(1L, actualResponse.getId());
        assertEquals(OrderStatus.CANCELED, actualResponse.getStatus());

        verify(orderService, times(1)).cancelOrder(eq(1L), any());
    }
}