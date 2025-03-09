package com.brokerage.api.dto.request;

import com.brokerage.api.model.OrderSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotBlank(message = "Asset name is required")
    private String assetName;

    @NotNull(message = "Order side is required")
    private OrderSide orderSide;

    @NotNull(message = "Size is required")
    @Positive(message = "Size must be positive")
    private Double size;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double price;
}
