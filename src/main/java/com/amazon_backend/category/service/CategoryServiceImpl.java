package com.amazon_backend.category.service;

import com.amazon_backend.category.dto.CategoryResponse;
import com.amazon_backend.category.dto.CreateCategoryRequest;
import com.amazon_backend.category.exception.CategoryAlreadyExistException;
import com.amazon_backend.category.exception.CategoryNotFoundException;
import com.amazon_backend.category.entity.Category;
import com.amazon_backend.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {

        if (categoryRepository.existsByName(request.getName())) {
            throw new CategoryAlreadyExistException("Category already exists");
        }

        Category category = new Category(request.getName());

        Category savedCategory = categoryRepository.save(category);

        return new CategoryResponse(
                savedCategory.getId(),
                savedCategory.getName()
        );
    }
    @Override

    public List<CategoryResponse> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName()))
                .toList();
    }
    @Override
    public CategoryResponse getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->

                        new CategoryNotFoundException("Category not found"));

        return new CategoryResponse(
                category.getId(),
                category.getName()
        );
    }
    @Override
    public void deleteCategory(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new CategoryNotFoundException("Category not found"));

        categoryRepository.delete(category);
    }

}
