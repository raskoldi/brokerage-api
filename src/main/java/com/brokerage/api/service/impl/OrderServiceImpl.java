package com.brokerage.api.service.impl;

import com.brokerage.api.dto.request.CreateOrderRequest;
import com.brokerage.api.dto.request.OrderFilterRequest;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.api.exception.ApiException;
import com.brokerage.api.exception.InsufficientFundsException;
import com.brokerage.api.exception.ResourceNotFoundException;
import com.brokerage.api.model.Asset;
import com.brokerage.api.model.Order;
import com.brokerage.api.model.OrderSide;
import com.brokerage.api.model.OrderStatus;
import com.brokerage.api.repository.AssetRepository;
import com.brokerage.api.repository.OrderRepository;
import com.brokerage.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final AssetRepository assetRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer ID: {}, asset: {}, side: {}, size: {}, price: {}",
                request.getCustomerId(), request.getAssetName(), request.getOrderSide(),
                request.getSize(), request.getPrice());

        // Validating customer has the asset or TRY (depending on BUY/SELL)
        if (request.getOrderSide() == OrderSide.BUY) {
            // Check if customer has enough TRY to buy
            Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(request.getCustomerId(), "TRY")
                    .orElseThrow(() -> new ResourceNotFoundException("Customer does not have TRY asset"));

            double requiredAmount = request.getSize() * request.getPrice();

            if (tryAsset.getUsableSize() < requiredAmount) {
                log.warn("Insufficient TRY balance for order. Required: {}, Available: {}",
                        requiredAmount, tryAsset.getUsableSize());
                throw new InsufficientFundsException("Insufficient TRY balance for this order");
            }

            log.info("Reserving {} TRY for order", requiredAmount);
            // Update TRY usable size
            tryAsset.setUsableSize(tryAsset.getUsableSize() - requiredAmount);
            assetRepository.save(tryAsset);
        } else if (request.getOrderSide() == OrderSide.SELL) {
            // Checking if customer has enough of the asset to sell
            Asset asset = assetRepository.findByCustomerIdAndAssetName(request.getCustomerId(), request.getAssetName())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer does not have the requested asset"));

            if (asset.getUsableSize() < request.getSize()) {
                log.warn("Insufficient asset balance for order. Required: {}, Available: {}",
                        request.getSize(), asset.getUsableSize());
                throw new InsufficientFundsException("Insufficient asset balance for this order");
            }

            log.info("Reserving {} units of {} for order", request.getSize(), request.getAssetName());
            // Updating asset usable size
            asset.setUsableSize(asset.getUsableSize() - request.getSize());
            assetRepository.save(asset);
        }

        // Creating and save order
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .assetName(request.getAssetName())
                .orderSide(request.getOrderSide())
                .size(request.getSize())
                .price(request.getPrice())
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        return mapToOrderResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getOrdersByCustomerIdAndDateRange(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting orders for customer ID: {} between {} and {}", customerId, startDate, endDate);
        return orderRepository.findByCustomerIdAndDateRange(customerId, startDate, endDate)
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> filterOrders(OrderFilterRequest request) {
        log.info("Filtering orders with request: {}", request);
        // For simplicity,  just filter by customerId and date range here
        LocalDateTime start = request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now().minusYears(10);
        LocalDateTime end = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();

        return getOrdersByCustomerIdAndDateRange(request.getCustomerId(), start, end);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long customerId) {
        log.info("Cancelling order with ID: {}, requested by customer ID: {}", orderId, customerId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Checking if user is admin (customerId will be null for admin)
        boolean isAdmin = (customerId == null);

        // Verifying the order belongs to the customer (unless admin)
        if (!isAdmin && !order.getCustomerId().equals(customerId)) {
            log.warn("Permission denied. Order belongs to customer ID: {}, requested by: {}",
                    order.getCustomerId(), customerId);
            throw new ApiException("You don't have permission to cancel this order");
        }

        // Only PENDING orders can be canceled
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Cannot cancel order with status: {}", order.getStatus());
            throw new ApiException("Only PENDING orders can be canceled");
        }

        // Updating the order status
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        log.info("Order status updated to CANCELED");

        // Return funds to the customer
        if (order.getOrderSide() == OrderSide.BUY) {
            // Return TRY to the customer
            Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), "TRY")
                    .orElseThrow(() -> new ResourceNotFoundException("Customer does not have TRY asset"));

            double returnAmount = order.getSize() * order.getPrice();
            tryAsset.setUsableSize(tryAsset.getUsableSize() + returnAmount);
            assetRepository.save(tryAsset);
            log.info("Returned {} TRY to customer", returnAmount);
        } else if (order.getOrderSide() == OrderSide.SELL) {
            // Return asset to the customer
            Asset asset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), order.getAssetName())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer does not have the asset"));

            asset.setUsableSize(asset.getUsableSize() + order.getSize());
            assetRepository.save(asset);
            log.info("Returned {} units of {} to customer", order.getSize(), order.getAssetName());
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse matchOrder(Long orderId) {
        log.info("Matching order with ID: {}", orderId);
        Order order = orderRepository.findByIdAndStatus(orderId, OrderStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Pending order not found"));

        // Updating order status
        order.setStatus(OrderStatus.MATCHED);
        orderRepository.save(order);
        log.info("Order status updated to MATCHED");

        // Updating customer assets
        if (order.getOrderSide() == OrderSide.BUY) {
            // Customer is buying an asset with TRY

            // First checking if customer already has the asset
            Asset asset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), order.getAssetName())
                    .orElse(Asset.builder()
                            .customerId(order.getCustomerId())
                            .assetName(order.getAssetName())
                            .size(0.0)
                            .usableSize(0.0)
                            .build());

            // Updating asset sizes
            asset.setSize(asset.getSize() + order.getSize());
            asset.setUsableSize(asset.getUsableSize() + order.getSize());
            assetRepository.save(asset);
            log.info("Added {} units of {} to customer's assets", order.getSize(), order.getAssetName());

            // TRY has already been deducted from usableSize, now update the actual size too
            Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), "TRY")
                    .orElseThrow(() -> new ResourceNotFoundException("Customer does not have TRY asset"));

            double spentAmount = order.getSize() * order.getPrice();
            tryAsset.setSize(tryAsset.getSize() - spentAmount);
            assetRepository.save(tryAsset);
            log.info("Deducted {} TRY from customer's balance", spentAmount);

        } else if (order.getOrderSide() == OrderSide.SELL) {
            // Customer is selling an asset for TRY

            // Updating sold asset size
            Asset asset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), order.getAssetName())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer does not have the asset"));

            asset.setSize(asset.getSize() - order.getSize());
            assetRepository.save(asset);
            log.info("Removed {} units of {} from customer's assets", order.getSize(), order.getAssetName());

            // Updating TRY asset
            Asset tryAsset = assetRepository.findByCustomerIdAndAssetName(order.getCustomerId(), "TRY")
                    .orElseThrow(() -> new ResourceNotFoundException("Customer does not have TRY asset"));

            double receivedAmount = order.getSize() * order.getPrice();
            tryAsset.setSize(tryAsset.getSize() + receivedAmount);
            tryAsset.setUsableSize(tryAsset.getUsableSize() + receivedAmount);
            assetRepository.save(tryAsset);
            log.info("Added {} TRY to customer's balance", receivedAmount);
        }

        return mapToOrderResponse(order);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .assetName(order.getAssetName())
                .orderSide(order.getOrderSide())
                .size(order.getSize())
                .price(order.getPrice())
                .status(order.getStatus())
                .createDate(order.getCreateDate())
                .build();
    }
}