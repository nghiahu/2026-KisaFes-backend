package org.example.backend.service;

import org.example.backend.dto.response.AdminUserDetailResponse;
import org.example.backend.dto.response.AdminUserResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.entity.Role;

import java.util.List;

public interface IAdminUserService {
    PageResponse<AdminUserResponse> getAllUsers(int page, int size, String search, String roleType, String status, String sortBy, String sortDirection);
    AdminUserDetailResponse getUserDetail(String userId);
    AdminUserResponse changeUserRole(String userId, String roleName);
    AdminUserResponse toggleUserStatus(String userId);
    void bulkAction(String action, List<String> userIds);
}
