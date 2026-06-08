package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.response.AdminUserDetailResponse;
import org.example.backend.dto.response.AdminUserResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.repository.IRoleRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.service.IAdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final org.example.backend.repository.IProjectRepository projectRepository;
    private final org.example.backend.repository.ITaskRepository taskRepository;
    private final org.example.backend.repository.ITeamMemberRepository teamMemberRepository;

    @Override
    public PageResponse<AdminUserResponse> getAllUsers(int page, int size, String search, String roleType, String status, String sortBy, String sortDirection) {
        List<User> searchResults;
        
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        if (search != null && !search.isBlank()) {
            searchResults = userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search);
            // Re-sort by custom logic
            searchResults.sort((a, b) -> {
                int result = 0;
                if ("fullName".equalsIgnoreCase(sortBy)) {
                    String nameA = a.getFullName() == null ? "" : a.getFullName();
                    String nameB = b.getFullName() == null ? "" : b.getFullName();
                    result = nameA.compareToIgnoreCase(nameB);
                } else if ("active".equalsIgnoreCase(sortBy)) {
                    result = Boolean.compare(a.isActive(), b.isActive());
                } else {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) result = 0;
                    else if (a.getCreatedAt() == null) result = -1;
                    else if (b.getCreatedAt() == null) result = 1;
                    else result = a.getCreatedAt().compareTo(b.getCreatedAt());
                }
                return direction == Sort.Direction.ASC ? result : -result;
            });
        } else {
            searchResults = userRepository.findAll(Sort.by(direction, sortBy));
        }

        // Apply filters
        searchResults = searchResults.stream().filter(u -> {
            // 1. Exclude Super Admin
            boolean isSuperAdmin = u.getRoles() != null && u.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getName()));
            if (isSuperAdmin) return false;

            // 2. Role filter
            if ("ADMIN".equalsIgnoreCase(roleType)) {
                if (u.getRoles() == null || u.getRoles().stream().noneMatch(r -> r.getName().toUpperCase().contains("ADMIN"))) return false;
            } else if ("USER".equalsIgnoreCase(roleType)) {
                if (u.getRoles() != null && u.getRoles().stream().anyMatch(r -> r.getName().toUpperCase().contains("ADMIN"))) return false;
            }

            // 3. Status filter
            if ("ACTIVE".equalsIgnoreCase(status)) {
                if (!u.isActive()) return false;
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                if (u.isActive()) return false;
            }

            return true;
        }).toList();

        int start = (page - 1) * size;
        int end = Math.min(start + size, searchResults.size());
        List<User> paginated = start < searchResults.size() ? searchResults.subList(start, end) : List.of();

        return PageResponse.<AdminUserResponse>builder()
                .content(paginated.stream().map(AdminUserResponse::fromEntity).toList())
                .page(page)
                .size(size)
                .totalElements(searchResults.size())
                .totalPages((int) Math.ceil((double) searchResults.size() / size))
                .build();
    }

    @Override
    public AdminUserDetailResponse getUserDetail(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));

        // Find all team ids for this user
        List<String> teamIds = teamMemberRepository.findByUserId(userId).stream()
                .map(org.example.backend.entity.TeamMember::getTeamId)
                .toList();

        // Start with direct projects
        Set<String> projectIds = new java.util.HashSet<>(
                user.getProjectIds() != null ? user.getProjectIds() : java.util.Collections.emptySet()
        );

        // Add projects where the user's teams are assigned
        if (!teamIds.isEmpty()) {
            projectRepository.findProjectsByTeamIds(teamIds)
                    .forEach(p -> projectIds.add(p.getId()));
        }
        
        long projectsJoined = projectIds.size();

        // Pending tasks: Tasks assigned to the user that are UNRESOLVED or have no resolution
        long pendingTasks = taskRepository.countByAssigneeIdAndResolutionIsNull(userId)
                + taskRepository.countByAssigneeIdAndResolution(userId, org.example.backend.entity.Resolution.UNRESOLVED);

        return AdminUserDetailResponse.builder()
                .user(AdminUserResponse.fromEntity(user))
                .stats(AdminUserDetailResponse.UserStats.builder()
                        .projectsJoined(projectsJoined)
                        .pendingTasks(pendingTasks)
                        .build())
                .build();
    }

    @Override
    public AdminUserResponse changeUserRole(String userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.ROLE_NOT_FOUND));
        user.setRoles(Set.of(role));
        userRepository.save(user);
        return AdminUserResponse.fromEntity(user);
    }

    @Override
    public AdminUserResponse toggleUserStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));
        user.setActive(!user.isActive());
        userRepository.save(user);
        return AdminUserResponse.fromEntity(user);
    }

    @Override
    public void bulkAction(String action, List<String> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        switch (action) {
            case "activate" -> users.forEach(u -> u.setActive(true));
            case "ban"      -> users.forEach(u -> u.setActive(false));
            case "delete"   -> { userRepository.deleteAllById(userIds); return; }
            default         -> throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR);
        }
        userRepository.saveAll(users);
    }
}
