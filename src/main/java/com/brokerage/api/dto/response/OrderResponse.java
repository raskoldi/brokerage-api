package com.brokerage.api.dto.response;
import com.brokerage.api.model.OrderSide;
import com.brokerage.api.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long customerId;
    private String assetName;
    private OrderSide orderSide;
    private Double size;
    private Double price;
    private OrderStatus status;
    private LocalDateTime createDate;
}
