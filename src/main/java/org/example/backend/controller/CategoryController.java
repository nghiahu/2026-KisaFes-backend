package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.CategoryRequest;
import org.example.backend.dto.response.CategoryResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.ICategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/categories")
public class CategoryController extends BaseController {

    private final ICategoryService categoryService;

    @GetMapping
    public ResponseEntity<ResponseWrapper<List<CategoryResponse>>> getAllCategories() {
        return success(categoryService.getAllCategories(), "Lấy danh sách category thành công");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseWrapper<CategoryResponse>> getCategoryById(@PathVariable String id) {
        return success(categoryService.getCategoryById(id), "Lấy chi tiết category thành công");
    }

    @PostMapping
    public ResponseEntity<ResponseWrapper<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return created(categoryService.createCategory(request), "Tạo category mới thành công");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseWrapper<CategoryResponse>> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request) {
        return success(categoryService.updateCategory(id, request), "Cập nhật category thành công");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseWrapper<Void>> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return success(null, "Xóa category thành công");
    }
}
