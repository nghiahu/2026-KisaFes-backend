package org.example.backend.service;

import org.example.backend.dto.request.BlogRequest;
import org.example.backend.dto.response.BlogResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.entity.Blog;

public interface IBlogService {
    PageResponse<BlogResponse> getBlogs(int page, int size, String search, Blog.BlogStatus status, String sortBy, String sortDir);
    BlogResponse getBlogById(String id);
    BlogResponse createBlog(BlogRequest request, String authorId, String authorName);
    BlogResponse updateBlog(String id, BlogRequest request);
    BlogResponse changeStatus(String id, Blog.BlogStatus status);
    void deleteBlog(String id);
    void publishScheduledBlogs();
}
