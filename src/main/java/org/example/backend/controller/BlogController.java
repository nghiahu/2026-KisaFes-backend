package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.BlogRequest;
import org.example.backend.dto.response.BlogResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.entity.Blog;
import org.example.backend.service.IBlogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/admin/blogs")
public class BlogController extends BaseController {

    private final IBlogService blogService;

    @GetMapping
    public ResponseEntity<ResponseWrapper<PageResponse<BlogResponse>>> getBlogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Blog.BlogStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return success(blogService.getBlogs(page, size, search, status, sortBy, sortDir), "Lấy danh sách blog thành công");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseWrapper<BlogResponse>> getBlogById(@PathVariable String id) {
        return success(blogService.getBlogById(id), "Lấy chi tiết blog thành công");
    }

    @PostMapping
    public ResponseEntity<ResponseWrapper<BlogResponse>> createBlog(
            @Valid @RequestBody BlogRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return created(blogService.createBlog(request, email, email), "Tạo blog thành công");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseWrapper<BlogResponse>> updateBlog(
            @PathVariable String id,
            @Valid @RequestBody BlogRequest request) {
        return success(blogService.updateBlog(id, request), "Cập nhật blog thành công");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ResponseWrapper<BlogResponse>> changeStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        Blog.BlogStatus status = Blog.BlogStatus.valueOf(body.get("status"));
        return success(blogService.changeStatus(id, status), "Cập nhật trạng thái blog thành công");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseWrapper<Void>> deleteBlog(@PathVariable String id) {
        blogService.deleteBlog(id);
        return success(null, "Xóa blog thành công");
    }
}
