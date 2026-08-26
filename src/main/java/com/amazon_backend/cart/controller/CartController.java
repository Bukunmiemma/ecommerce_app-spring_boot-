package com.amazon_backend.cart.controller;

import com.amazon_backend.cart.dto.AddToCartRequest;
import com.amazon_backend.cart.dto.CartResponse;
import com.amazon_backend.cart.dto.UpdateCartItemRequest;
import com.amazon_backend.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }
    //Get logged-in user's cart
      @GetMapping
      public ResponseEntity<CartResponse> getCart (
              Authentication authentication){
        String email = authentication.getName();
        return ResponseEntity.ok(cartService.getCart(email));
      }

      //Add product to cart
      @PostMapping("/items")
      public ResponseEntity<CartResponse> addToCart(@RequestBody
                                                    AddToCartRequest request,
                                                    Authentication authentication
                                                    ){
        String email = authentication.getName();
          return ResponseEntity.ok(cartService.addToCart(email, request));

      }
      //Update cart item quantity
      @PutMapping("/items/{itemId}")
      public ResponseEntity<CartResponse> updateCartItem(
              @PathVariable Long itemId,
              @RequestBody UpdateCartItemRequest request,
Authentication authentication){
          String email = authentication.getName();
          return ResponseEntity.ok(cartService.updateCartItem(email, itemId, request));
      }
      //Remove one item
      @DeleteMapping("/items/{itemId}")
      public ResponseEntity<Void> removeCartItem(
              @PathVariable Long itemId,
              Authentication authentication ){
          String email = authentication.getName();
          return ResponseEntity.noContent().build();
      }
      // Clear entire cart
      @DeleteMapping
      public ResponseEntity<Void> clearCart(Authentication authentication){
          String email = authentication.getName();
             cartService.clearCart(email);
             return  ResponseEntity.noContent().build();
      }


}
