package com.amazon_backend.product.service;

import com.amazon_backend.category.entity.Category;
import com.amazon_backend.category.exception.CategoryNotFoundException;
import com.amazon_backend.category.repository.CategoryRepository;
import com.amazon_backend.product.dto.ProductRequest;
import com.amazon_backend.product.dto.ProductResponse;
import com.amazon_backend.product.entity.Product;
import com.amazon_backend.product.exception.ProductAlreadyExistsException;
import com.amazon_backend.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;



    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;

    }
    private ProductResponse mapToProductResponse (Product product){
        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setStockQuantity(product.getStockQuantity());

        Set<String> categoryNames = product.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());
        response.setCategories(categoryNames);
        return response;
    }

    @Override
    public ProductResponse createProduct(ProductRequest request){
        if(productRepository.existsByName(request.getName())){
            throw new ProductAlreadyExistsException("Product already exists");
        }
        List<Category> categories =categoryRepository.findAllById(request.getCategoryIds());
        if(categories.size() != request.getCategoryIds().size()){
            throw new CategoryNotFoundException("One or more categories do not exist.");
        }
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategories(new HashSet<>(categories));

        Product savedProduct = productRepository.save(product);

return mapToProductResponse(savedProduct);
    }

    @Override
    public List<ProductResponse> getAllProducts() {

        List<Product> products = productRepository.findAll();
        return products.stream().map(this::mapToProductResponse).collect(Collectors.toList());
    }


}
