package com.amazon_backend.product.service;

import com.amazon_backend.product.dto.ProductRequest;
import com.amazon_backend.product.dto.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct (ProductRequest request);

    List<ProductResponse> getAllProducts();

}
