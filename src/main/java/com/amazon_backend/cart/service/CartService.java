package com.amazon_backend.cart.service;

import com.amazon_backend.cart.dto.AddToCartRequest;
import com.amazon_backend.cart.dto.CartResponse;
import com.amazon_backend.cart.dto.UpdateCartItemRequest;

public interface CartService {
    CartResponse getCart (String email);

    CartResponse addToCart (String email, AddToCartRequest request);

    CartResponse updateCartItem (String email, Long itemId, UpdateCartItemRequest request);

    void removeCartItem (String email, Long itemId);

    void clearCart(String email);
}
