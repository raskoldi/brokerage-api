package com.brokerage.api.dto.request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchOrderRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;
}
