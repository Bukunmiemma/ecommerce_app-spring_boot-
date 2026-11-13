package com.amazon_backend.product.service;

import com.amazon_backend.category.entity.Category;
import com.amazon_backend.category.exception.CategoryNotFoundException;
import com.amazon_backend.category.repository.CategoryRepository;
import com.amazon_backend.product.dto.ProductRequest;
import com.amazon_backend.product.dto.ProductResponse;
import com.amazon_backend.product.dto.UpdateProductRequest;
import com.amazon_backend.product.entity.Product;
import com.amazon_backend.product.exception.ProductAlreadyExistsException;
import com.amazon_backend.product.exception.ProductNotFoundException;
import com.amazon_backend.product.repository.ProductRepository;
import com.amazon_backend.store.entity.Store;
import com.amazon_backend.store.entity.StoreStatus;
import com.amazon_backend.store.repositories.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional

public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                                StoreRepository storeRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;

    }

    private Product getProductOwnedByUser(Long id, String email) {
        return productRepository.findByIdAndStoreOwnerEmail(id, email)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found or you do not own this product"
                ));
    }
    private ProductResponse mapToProductResponse (Product product){
        ProductResponse response = new ProductResponse();

        response.setProductId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());

        Set<String> categoryNames = product.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());
        response.setCategories(categoryNames);
        response.setStoreId(product.getStore().getId());
        response.setStoreName(product.getStore().getName());
        return response;
    }


    @Override
    public ProductResponse createProduct(
            Long storeId,
            String email,
            ProductRequest request){
        // 1. Verify that the authenticated user owns this store
        Store store = storeRepository.findByIdAndOwnerEmail(storeId, email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Store not found or you do not own this store"
                ));
        // Prevent inactive stores from publishing new products.

        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your store is not active and cannot publish products"
            );
        }

        // 2. Check whether this product name already exists in this store
        if (productRepository.existsByNameAndStoreId(
                request.getName(), storeId)) {
            throw new ProductAlreadyExistsException(
                    "Product already exists in this store"
            );
        }

        // 3. Validate the selected categories
        List<Category> categories =categoryRepository.findAllById(request.getCategoryIds());
        if(categories.size() != request.getCategoryIds().size()){
            throw new CategoryNotFoundException("One or more categories do not exist.");
        }
        //4. Create the product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        // Associate the product with its owner's store
       // This establishes the relationship between the product and its store.
       // When Hibernate saves the product, it stores the store's ID in the store_id column.
        //This is how our marketplace will know which vendor owns each product.
        product.setStore(store);
        product.setCategories(new HashSet<>(categories));

        Product savedProduct = productRepository.save(product);

return mapToProductResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {

        return productRepository
                .findByActiveTrueAndStore_Status(StoreStatus.ACTIVE)
                .stream()
                .map(this::mapToProductResponse)
                .toList();
    }

@Override
@Transactional(readOnly = true)
    public ProductResponse getProductById(Long id){
    Product product = productRepository
            .findByIdAndActiveTrueAndStore_Status(
                    id,
                    StoreStatus.ACTIVE
            )
            .orElseThrow(() -> new ProductNotFoundException(
                    "Product not found"
            ));

    return mapToProductResponse(product);
}

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByStoreId(Long storeId) {

        boolean storeExists = storeRepository
                .findByIdAndStatus(storeId, StoreStatus.ACTIVE)
                .isPresent();

        if (!storeExists) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Store not found"
            );
        }

        return productRepository
                .findByStore_IdAndActiveTrueAndStore_Status(
                        storeId,
                        StoreStatus.ACTIVE
                )
                .stream()
                .map(this::mapToProductResponse)
                .toList();
    }
    @Override
    public ProductResponse updateProduct(
            Long id,
            String email,
            UpdateProductRequest request
    ) {
        Product product = getProductOwnedByUser(id, email);

        // Check whether another product in this store uses the requested name
        productRepository.findByNameAndStoreId(
                request.getName(),
                product.getStore().getId()
        ).ifPresent(existingProduct -> {
            if (!existingProduct.getId().equals(id)) {
                throw new ProductAlreadyExistsException(
                        "Another product with this name already exists in your store"
                );
            }
        });

        Set<Category> categories = new HashSet<>(
                categoryRepository.findAllById(request.getCategoryIds())
        );

        if (categories.size() != request.getCategoryIds().size()) {
            throw new CategoryNotFoundException(
                    "One or more categories do not exist"
            );
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategories(categories);

        Product updatedProduct = productRepository.save(product);

        return mapToProductResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id, String email) {

        Product product = getProductOwnedByUser(id, email);
        product.setActive(false);

        productRepository.save(product);
        //Now deleting a product means making it unavailable for purchase, without destroying its order history.
    }

    @Override
    public ProductResponse restockProduct(
            Long id,
            String email,
            Integer quantity
    ) {
        Product product = getProductOwnedByUser(id, email);

        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Restock quantity must be greater than zero"
            );
        }

        int currentStock = product.getStockQuantity() == null
                ? 0
                : product.getStockQuantity();

        product.setStockQuantity(currentStock + quantity);

        Product updatedProduct = productRepository.save(product);

        return mapToProductResponse(updatedProduct);
    }


}
