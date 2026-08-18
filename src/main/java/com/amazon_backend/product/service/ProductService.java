package com.amazon_backend.product.service;

import com.amazon_backend.product.dto.ProductRequest;
import com.amazon_backend.product.dto.ProductResponse;
import com.amazon_backend.product.dto.UpdateProductRequest;
import com.amazon_backend.product.entity.Product;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct (ProductRequest request);

    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);

}
