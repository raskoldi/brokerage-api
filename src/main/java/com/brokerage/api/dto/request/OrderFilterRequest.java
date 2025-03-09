package com.brokerage.api.dto.request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterRequest {
    private Long customerId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String assetName;
    private String status;
}
