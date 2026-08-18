package com.amazon_backend.product.controller;


import com.amazon_backend.product.dto.ProductRequest;
import com.amazon_backend.product.dto.ProductResponse;
import com.amazon_backend.product.dto.UpdateProductRequest;
import com.amazon_backend.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct (
            @Valid @RequestBody
            ProductRequest request
    ){
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(){
        List<ProductResponse> products = productService.getAllProducts();
        return  ResponseEntity.ok(products);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long id
    ){
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }
     @PutMapping("/{id}")
     public ResponseEntity<ProductResponse> updateProduct(
             @PathVariable Long id,
             @Valid @RequestBody
             UpdateProductRequest request
     ){
        return ResponseEntity.ok(
                productService.updateProduct(id,request)
        );
     }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct( @PathVariable Long id){
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();


    }

}
