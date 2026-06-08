package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.response.AdminAuditLogResponse;
import org.example.backend.dto.response.AdminAnalyticsResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.entity.AuditLog;
import org.example.backend.entity.Blog;
import org.example.backend.entity.User;
import org.example.backend.repository.IAuditLogRepository;
import org.example.backend.repository.IBlogRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.service.IAdminAnalyticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements IAdminAnalyticsService {

    private final IUserRepository userRepository;
    private final IBlogRepository blogRepository;
    private final IAuditLogRepository auditLogRepository;

    @Override
    public AdminAnalyticsResponse getDashboard() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(User::isActive).count();
        long totalBlogs = blogRepository.count();
        long publishedBlogs = blogRepository.findByStatus(Blog.BlogStatus.PUBLISHED,
                PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();

        // User trends: last 6 months grouped by month
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM");
        Map<String, Long> userByMonth = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null &&
                             u.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(6)))
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().format(fmt), Collectors.counting()));

        List<AdminAnalyticsResponse.MonthlyUserStat> userTrends = userByMonth.entrySet().stream()
                .map(e -> AdminAnalyticsResponse.MonthlyUserStat.builder()
                        .month(e.getKey()).users(e.getValue()).build())
                .toList();

        // Blog by status
        List<AdminAnalyticsResponse.BlogStatusStat> blogsByStatus = List.of(
                AdminAnalyticsResponse.BlogStatusStat.builder().name("DRAFT")
                        .value(blogRepository.findByStatus(Blog.BlogStatus.DRAFT, PageRequest.of(0, 1)).getTotalElements()).build(),
                AdminAnalyticsResponse.BlogStatusStat.builder().name("PENDING")
                        .value(blogRepository.findByStatus(Blog.BlogStatus.PENDING, PageRequest.of(0, 1)).getTotalElements()).build(),
                AdminAnalyticsResponse.BlogStatusStat.builder().name("PUBLISHED")
                        .value(publishedBlogs).build(),
                AdminAnalyticsResponse.BlogStatusStat.builder().name("ARCHIVED")
                        .value(blogRepository.findByStatus(Blog.BlogStatus.ARCHIVED, PageRequest.of(0, 1)).getTotalElements()).build()
        );

        return AdminAnalyticsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalProjects(0) // can extend later
                .totalBlogs(totalBlogs)
                .publishedBlogs(publishedBlogs)
                .userTrends(userTrends)
                .blogsByStatus(blogsByStatus)
                .build();
    }

    @Override
    public PageResponse<AdminAuditLogResponse> getAuditLogs(int page, int size, String search, AuditLog.AuditAction action) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("timestamp").descending());
        Page<AuditLog> logs;

        boolean hasSearch = search != null && !search.isBlank();
        boolean hasAction = action != null;

        if (hasSearch) {
            logs = auditLogRepository
                    .findByUserNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrDetailsContainingIgnoreCase(
                            search, search, search, pageable);
        } else if (hasAction) {
            logs = auditLogRepository.findByAction(action, pageable);
        } else {
            logs = auditLogRepository.findAll(pageable);
        }

        return PageResponse.<AdminAuditLogResponse>builder()
                .content(logs.getContent().stream().map(AdminAuditLogResponse::fromEntity).toList())
                .page(page)
                .size(size)
                .totalElements(logs.getTotalElements())
                .totalPages(logs.getTotalPages())
                .build();
    }
}
