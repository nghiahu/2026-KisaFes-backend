package org.example.backend.service;

import org.example.backend.dto.request.CategoryRequest;
import org.example.backend.dto.response.CategoryResponse;

import java.util.List;

public interface ICategoryService {
    List<CategoryResponse> getAllCategories();
    CategoryResponse getCategoryById(String id);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(String id, CategoryRequest request);
    void deleteCategory(String id);
}
