package com.brokerage.api.service;
import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.api.exception.InsufficientFundsException;
import com.brokerage.api.exception.ResourceNotFoundException;
import com.brokerage.api.model.Asset;
import com.brokerage.api.model.Order;
import com.brokerage.api.model.OrderSide;
import com.brokerage.api.model.OrderStatus;
import com.brokerage.api.repository.AssetRepository;
import com.brokerage.api.repository.OrderRepository;
import com.brokerage.api.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Long customerId;
    private Asset tryAsset;
    private Asset stockAsset;
    private Order pendingOrder;
    private CreateOrderRequest buyRequest;
    private CreateOrderRequest sellRequest;

    @BeforeEach
    void setUp() {
        customerId = 1L;

        // TRY asset
        tryAsset = Asset.builder()
                .id(1L)
                .customerId(customerId)
                .assetName("TRY")
                .size(10000.0)
                .usableSize(10000.0)
                .build();

        // stock asset
        stockAsset = Asset.builder()
                .id(2L)
                .customerId(customerId)
                .assetName("AAPL")
                .size(100.0)
                .usableSize(100.0)
                .build();

        // pending order
        pendingOrder = Order.builder()
                .id(1L)
                .customerId(customerId)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(10.0)
                .price(150.0)
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        // order requests
        buyRequest = CreateOrderRequest.builder()
                .customerId(customerId)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(10.0)
                .price(150.0)
                .build();

        sellRequest = CreateOrderRequest.builder()
                .customerId(customerId)
                .assetName("AAPL")
                .orderSide(OrderSide.SELL)
                .size(10.0)
                .price(150.0)
                .build();
    }

    @Test
    void createOrder_BuyOrder_Success() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "TRY")).thenReturn(Optional.of(tryAsset));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        OrderResponse response = orderService.createOrder(buyRequest);

        // Then
        assertNotNull(response);
        assertEquals(buyRequest.getAssetName(), response.getAssetName());
        assertEquals(buyRequest.getOrderSide(), response.getOrderSide());
        assertEquals(buyRequest.getSize(), response.getSize());
        assertEquals(buyRequest.getPrice(), response.getPrice());
        assertEquals(OrderStatus.PENDING, response.getStatus());

        // Verify TRY usable size was reduced
        verify(assetRepository).save(argThat(asset ->
                asset.getAssetName().equals("TRY") &&
                        asset.getUsableSize() == 8500.0)); // 10000 - (10 * 150)
    }

    @Test
    void createOrder_BuyOrder_InsufficientFunds() {
        // Given
        Asset lowBalanceTry = Asset.builder()
                .id(1L)
                .customerId(customerId)
                .assetName("TRY")
                .size(100.0)
                .usableSize(100.0)
                .build();

        when(assetRepository.findByCustomerIdAndAssetName(customerId, "TRY")).thenReturn(Optional.of(lowBalanceTry));

        // When & Then
        assertThrows(InsufficientFundsException.class, () -> orderService.createOrder(buyRequest));

        // not called to save anything
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_SellOrder_Success() {
        // Given
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "AAPL")).thenReturn(Optional.of(stockAsset));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        OrderResponse response = orderService.createOrder(sellRequest);

        // Then
        assertNotNull(response);
        assertEquals(sellRequest.getAssetName(), response.getAssetName());
        assertEquals(sellRequest.getOrderSide(), response.getOrderSide());
        assertEquals(sellRequest.getSize(), response.getSize());
        assertEquals(sellRequest.getPrice(), response.getPrice());
        assertEquals(OrderStatus.PENDING, response.getStatus());

        // stock usable size was reduced
        verify(assetRepository).save(argThat(asset ->
                asset.getAssetName().equals("AAPL") &&
                        asset.getUsableSize() == 90.0)); // 100 - 10
    }

    @Test
    void createOrder_SellOrder_InsufficientAssets() {
        // Given
        Asset lowBalanceStock = Asset.builder()
                .id(2L)
                .customerId(customerId)
                .assetName("AAPL")
                .size(5.0)
                .usableSize(5.0)
                .build();

        when(assetRepository.findByCustomerIdAndAssetName(customerId, "AAPL")).thenReturn(Optional.of(lowBalanceStock));

        // When & Then
        assertThrows(InsufficientFundsException.class, () -> orderService.createOrder(sellRequest));

        // not called to save anything
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_PendingBuyOrder_Success() {
        // Given
        Order buyOrder = Order.builder()
                .id(1L)
                .customerId(customerId)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(10.0)
                .price(150.0)
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(buyOrder));
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "TRY")).thenReturn(Optional.of(tryAsset));
        when(orderRepository.save(any(Order.class))).thenReturn(buyOrder);

        // When
        OrderResponse response = orderService.cancelOrder(1L, customerId);

        // Then
        assertNotNull(response);
        assertEquals(OrderStatus.CANCELED, response.getStatus());

        // TRY usable size restored
        verify(assetRepository).save(argThat(asset ->
                asset.getAssetName().equals("TRY") &&
                        asset.getUsableSize() == 11500.0)); // 10000 + (10 * 150)
    }

    @Test
    void cancelOrder_PendingSellOrder_Success() {
        // Given
        Order sellOrder = Order.builder()
                .id(1L)
                .customerId(customerId)
                .assetName("AAPL")
                .orderSide(OrderSide.SELL)
                .size(10.0)
                .price(150.0)
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(sellOrder));
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "AAPL")).thenReturn(Optional.of(stockAsset));
        when(orderRepository.save(any(Order.class))).thenReturn(sellOrder);

        // When
        OrderResponse response = orderService.cancelOrder(1L, customerId);

        // Then
        assertNotNull(response);
        assertEquals(OrderStatus.CANCELED, response.getStatus());

        // stock usable size restored
        verify(assetRepository).save(argThat(asset ->
                asset.getAssetName().equals("AAPL") &&
                        asset.getUsableSize() == 110.0)); // 100 + 10
    }

    @Test
    void matchOrder_BuyOrder_Success() {
        // Given
        when(orderRepository.findByIdAndStatus(1L, OrderStatus.PENDING)).thenReturn(Optional.of(pendingOrder));
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "AAPL")).thenReturn(Optional.of(stockAsset));
        when(assetRepository.findByCustomerIdAndAssetName(customerId, "TRY")).thenReturn(Optional.of(tryAsset));
        when(orderRepository.save(any(Order.class))).thenReturn(pendingOrder);

        // When
        OrderResponse response = orderService.matchOrder(1L);

        // Then
        assertNotNull(response);
        assertEquals(OrderStatus.MATCHED, response.getStatus());

        // TRY size reduced
        verify(assetRepository).save(argThat(asset ->
                asset.getAssetName().equals("TRY") &&
                        asset.getSize() == 8500.0)); // 10000 - (10 * 150)

        // stock size increased
        verify(assetRepository).save(argThat(asset ->
                asset.getAssetName().equals("AAPL") &&
                        asset.getSize() == 110.0 &&
                        asset.getUsableSize() == 110.0)); // 100 + 10
    }

    @Test
    void getOrdersByCustomerIdAndDateRange_Success() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        List<Order> orders = Arrays.asList(
                Order.builder().id(1L).customerId(customerId).build(),
                Order.builder().id(2L).customerId(customerId).build()
        );

        when(orderRepository.findByCustomerIdAndDateRange(customerId, startDate, endDate)).thenReturn(orders);

        // When
        List<OrderResponse> responses = orderService.getOrdersByCustomerIdAndDateRange(customerId, startDate, endDate);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
    }
}
