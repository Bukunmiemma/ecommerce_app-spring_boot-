package com.amazon_backend.product.service;

import com.amazon_backend.product.dto.ProductRequest;
import com.amazon_backend.product.dto.ProductResponse;
import com.amazon_backend.product.dto.UpdateProductRequest;
import com.amazon_backend.product.entity.Product;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct ( Long storeId, String email,ProductRequest request);

    //storeId: identifies the store receiving the product.
    //email: identifies the authenticated vendor.
    //request: contains the product details.
    //The service will use the email from the authenticated JWT to verify that the vendor owns the requested store.


    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(
            Long id,
            String email,
            UpdateProductRequest request
    );

    void deleteProduct(Long id, String email);

    ProductResponse restockProduct(
            Long id,
            String email,
            Integer quantity
    );
    //Why are we adding email?
    //Every operation must know which authenticated user is making the request.
    //For example: updateProduct(10L, "vendor@gmail.com", request);

    List<ProductResponse> getProductsByStoreId(Long storeId);
}

//What we've secured so far
//1. Product stock cannot be NULL.
//2. Product prices cannot be NULL.
//3. Order items preserve historical prices.
//4. Product stock is locked during checkout.
//5. The same cart cannot be processed by two simultaneous checkout requests using our new locking method.
//6. Payment status is tracked separately from order status.

//Your cart now has two layers of protection:
//1. User-row locking: prevents simultaneous first-time cart creation for the same user.
//2. Cart-row locking: serializes cart modifications and checkout operations for an existing cart.