package com.amazon_backend.product.dto;

import jakarta.validation.constraints.Min;

public class RestockRequest {
    @Min(value =1,message = "Restock quantity must be at least 1")
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
