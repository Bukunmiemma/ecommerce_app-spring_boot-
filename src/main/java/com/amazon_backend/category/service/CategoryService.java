package com.amazon_backend.category.service;

import com.amazon_backend.category.dto.CategoryResponse;
import com.amazon_backend.category.dto.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CreateCategoryRequest request);

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long id);

    void deleteCategory(Long id);
}
