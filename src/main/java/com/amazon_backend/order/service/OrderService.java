package com.amazon_backend.order.service;

import com.amazon_backend.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {


    //checkout is for creating order by customers
    //getOrderById is to see one order
    //getMyOrders is to see order history
    //Our API will support something like
    //  POST /api/orders/checkout
    //  GET  /api/orders/{orderId}
    //  GET  /api/orders
    OrderResponse checkout (String email);
    OrderResponse getOrderById(String email, Long orderId);
    List<OrderResponse> getMyOrders(String email);

}
