package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.response.BlogResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.entity.Blog;
import org.example.backend.service.IBlogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/public/blogs")
public class PublicBlogController extends BaseController {

    private final IBlogService blogService;

    @GetMapping
    public ResponseEntity<ResponseWrapper<PageResponse<BlogResponse>>> getBlogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "publishAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        // Only return published blogs for public API
        return success(blogService.getBlogs(page, size, search, Blog.BlogStatus.PUBLISHED, sortBy, sortDir), "Lấy danh sách blog thành công");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseWrapper<BlogResponse>> getBlogById(@PathVariable String id) {
        return success(blogService.getBlogById(id), "Lấy chi tiết blog thành công");
    }
}
