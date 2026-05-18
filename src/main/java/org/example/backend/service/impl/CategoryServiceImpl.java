package org.example.backend.service.impl;

import org.example.backend.common.base.BaseServiceImpl;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.CategoryRequest;
import org.example.backend.dto.response.CategoryResponse;
import org.example.backend.entity.Category;
import org.example.backend.repository.ICategoryRepository;
import org.example.backend.service.ICategoryService;
import org.example.backend.entity.Permission;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends BaseServiceImpl<Category, String> implements ICategoryService {

    private final ICategoryRepository categoryRepository;

    public CategoryServiceImpl(ICategoryRepository categoryRepository) {
        super(categoryRepository);
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS);
        }
        Category category = request.toEntity();
        
        validateCategory(category);
        ensureOwnerRole(category);
        
        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.fromEntity(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(String id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        categoryRepository.findByName(request.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new CustomBusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS);
                    }
                });

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        
        if (request.getDefaultStatuses() != null) {
            category.setDefaultStatuses(request.getDefaultStatuses().stream()
                    .map(CategoryRequest.CategoryStatusRequest::toEntity)
                    .toList());
        }
        
        if (request.getDefaultBoardColumns() != null) {
            category.setDefaultBoardColumns(request.getDefaultBoardColumns().stream()
                    .map(CategoryRequest.CategoryBoardColumnRequest::toEntity)
                    .toList());
        }
        
        if (request.getDefaultRoles() != null) {
            category.setDefaultRoles(request.getDefaultRoles().stream()
                    .map(CategoryRequest.CategoryRoleRequest::toEntity)
                    .toList());
        }

        validateCategory(category);
        ensureOwnerRole(category);

        Category updatedCategory = categoryRepository.save(category);
        return CategoryResponse.fromEntity(updatedCategory);
    }

    @Override
    public void deleteCategory(String id) {
        if (!categoryRepository.existsById(id)) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        categoryRepository.deleteById(id);
    }

    private void validateCategory(Category category) {
        if (category.getDefaultBoardColumns() != null) {
            Set<String> columnNames = category.getDefaultBoardColumns().stream()
                    .map(Category.CategoryBoardColumn::getName)
                    .collect(Collectors.toSet());
            if (columnNames.size() < category.getDefaultBoardColumns().size()) {
                throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Tên cột trong bảng không được trùng nhau");
            }
        }

        if (category.getDefaultRoles() != null) {
            Set<String> roleNames = category.getDefaultRoles().stream()
                    .map(Category.CategoryRole::getName)
                    .collect(Collectors.toSet());
            if (roleNames.size() < category.getDefaultRoles().size()) {
                throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Tên vai trò không được trùng nhau");
            }
        }
    }

    private void ensureOwnerRole(Category category) {
        if (category.getDefaultRoles() == null) {
            category.setDefaultRoles(new ArrayList<>());
        } else {
            // Need to convert to a mutable list if it's immutable (e.g., from Stream.toList())
            category.setDefaultRoles(new ArrayList<>(category.getDefaultRoles()));
        }

        boolean hasOwner = category.getDefaultRoles().stream()
                .anyMatch(r -> "Owner (default)".equalsIgnoreCase(r.getName()));

        if (!hasOwner) {
            Category.CategoryRole ownerRole = new Category.CategoryRole();
            ownerRole.setName("Owner (default)");
            ownerRole.setPermissions(Arrays.stream(Permission.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet()));
            category.getDefaultRoles().add(ownerRole);
        } else {
            // Ensure existing Owner role has all permissions
            category.getDefaultRoles().forEach(r -> {
                if ("Owner (default)".equalsIgnoreCase(r.getName())) {
                    r.setPermissions(Arrays.stream(Permission.values())
                            .map(Enum::name)
                            .collect(Collectors.toSet()));
                }
            });
        }
    }
}
