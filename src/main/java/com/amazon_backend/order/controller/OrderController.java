package com.amazon_backend.order.controller;
import com.amazon_backend.order.dto.OrderResponse;
import com.amazon_backend.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(Authentication authentication){
        String email = authentication.getName();
        OrderResponse response = orderService.checkout(email);
        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication){
        String email = authentication.getName();
        List<OrderResponse> orders= orderService.getMyOrders(email);
        return ResponseEntity.ok(orders);
    }
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication){
        String email = authentication.getName();
        OrderResponse order = orderService.getOrderById(email, orderId);
        return ResponseEntity.ok(order);
    }
}
