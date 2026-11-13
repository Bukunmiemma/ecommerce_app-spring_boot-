package com.amazon_backend.product.controller;
import com.amazon_backend.product.dto.ProductRequest;
import com.amazon_backend.product.dto.ProductResponse;
import com.amazon_backend.product.dto.RestockRequest;
import com.amazon_backend.product.dto.UpdateProductRequest;
import com.amazon_backend.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService


                             ) {
        this.productService = productService;

    }

    // Create product
    //@PathVariable Long storeId: receives the store ID from the URL.
    //@AuthenticationPrincipal: obtains the authenticated user's security principal.
    //userDetails.getUsername(): retrieves the email associated with the authenticated user.
    // productService.createProduct(...): passes all three values to the service.
    // POST /api/products/store/1. The 1 represents the store ID.
    @PostMapping("/store/{storeId}")
    public ResponseEntity<ProductResponse> createProduct(
            @PathVariable Long storeId,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProductResponse response = productService.createProduct(
                storeId,
                userDetails.getUsername(),
                request
        );
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

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<ProductResponse>> getProductsByStore(
            @PathVariable Long storeId
    ) {

        return ResponseEntity.ok(
                productService.getProductsByStoreId(storeId)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(
                productService.updateProduct(
                        id,
                        userDetails.getUsername(),
                        request
                )
        );
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        productService.deleteProduct(id, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

    // Restock Product Admin only

    @PatchMapping("/{id}/restock")
    public ResponseEntity<ProductResponse> restockProduct(
            @PathVariable Long id,
            @Valid @RequestBody RestockRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProductResponse response = productService.restockProduct(
                id,
                userDetails.getUsername(),
                request.getQuantity()
        );

        return ResponseEntity.ok(response);
    }


}
