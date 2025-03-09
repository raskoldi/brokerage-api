package com.brokerage.api.controller;
import com.brokerage.api.dto.request.MatchOrderRequest;
import com.brokerage.api.dto.response.OrderResponse;
import com.brokerage.api.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final OrderService orderService;

    @PostMapping("/orders/match")
    public ResponseEntity<OrderResponse> matchOrder(@Valid @RequestBody MatchOrderRequest request) {
        return ResponseEntity.ok(orderService.matchOrder(request.getOrderId()));
    }
}
