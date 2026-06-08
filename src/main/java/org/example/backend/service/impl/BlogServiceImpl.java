package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.BlogRequest;
import org.example.backend.dto.response.BlogResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.entity.Blog;
import org.example.backend.repository.IBlogRepository;
import org.example.backend.service.IBlogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements IBlogService {

    private final IBlogRepository blogRepository;

    @Override
    public PageResponse<BlogResponse> getBlogs(int page, int size, String search, Blog.BlogStatus status, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));
        Page<Blog> blogs;

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasStatus = status != null;

        if (hasSearch && hasStatus) {
            blogs = blogRepository.findBySearchAndStatus(search, status, pageable);
        } else if (hasSearch) {
            blogs = blogRepository.findBySearch(search, pageable);
        } else if (hasStatus) {
            blogs = blogRepository.findByStatus(status, pageable);
        } else {
            blogs = blogRepository.findAll(pageable);
        }

        return PageResponse.<BlogResponse>builder()
                .content(blogs.getContent().stream().map(BlogResponse::fromEntity).toList())
                .page(page)
                .size(size)
                .totalElements(blogs.getTotalElements())
                .totalPages(blogs.getTotalPages())
                .build();
    }

    @Override
    public BlogResponse getBlogById(String id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return BlogResponse.fromEntity(blog);
    }

    @Override
    public BlogResponse createBlog(BlogRequest request, String authorId, String authorName) {
        Blog blog = Blog.builder()
                .title(request.getTitle())
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .thumbnailUrl(request.getThumbnailUrl())
                .status(request.getStatus() != null ? request.getStatus() : Blog.BlogStatus.DRAFT)
                .publishAt(request.getPublishAt())
                .tags(request.getTags())
                .authorId(authorId)
                .authorName(authorName)
                .build();
        return BlogResponse.fromEntity(blogRepository.save(blog));
    }

    @Override
    public BlogResponse updateBlog(String id, BlogRequest request) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        blog.setTitle(request.getTitle());
        blog.setExcerpt(request.getExcerpt());
        blog.setContent(request.getContent());
        if (request.getThumbnailUrl() != null) blog.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getStatus() != null) blog.setStatus(request.getStatus());
        if (request.getTags() != null) blog.setTags(request.getTags());
        blog.setPublishAt(request.getPublishAt());
        return BlogResponse.fromEntity(blogRepository.save(blog));
    }

    @Override
    public BlogResponse changeStatus(String id, Blog.BlogStatus status) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        blog.setStatus(status);
        return BlogResponse.fromEntity(blogRepository.save(blog));
    }

    @Override
    public void deleteBlog(String id) {
        if (!blogRepository.existsById(id)) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        blogRepository.deleteById(id);
    }

    @Override
    public void publishScheduledBlogs() {
        LocalDateTime now = LocalDateTime.now();
        List<Blog> scheduledBlogs = blogRepository.findByStatusAndPublishAtBefore(Blog.BlogStatus.PENDING, now);
        
        if (!scheduledBlogs.isEmpty()) {
            scheduledBlogs.forEach(blog -> blog.setStatus(Blog.BlogStatus.PUBLISHED));
            blogRepository.saveAll(scheduledBlogs);
        }
    }
}
