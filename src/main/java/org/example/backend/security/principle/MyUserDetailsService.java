package org.example.backend.security.principle;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.*;
import org.example.backend.repository.IProjectMemberRepository;
import org.example.backend.repository.IProjectRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.service.RedisService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {
    private final IUserRepository userRepository;
    private final IProjectMemberRepository projectMemberRepository;
    private final IProjectRepository projectRepository;
    private final RedisService redisService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String cacheKey = "auth:user:v2:" + identifier;

        // 1. Kiểm tra Cache từ Redis
        Object cachedUser = redisService.getObject(cacheKey);
        if (cachedUser instanceof MyUserDetails myUserDetails) {
            return myUserDetails;
        }

        // 2. Nếu không có trong Cache, truy vấn MongoDB
        User user = userRepository.findByUserNameOrEmail(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + identifier));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 3. Nạp Quyền Hệ Thống
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role.getName() != null && !role.getName().isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
                }
                if (role.getPermissions() != null) {
                    for (SystemPermission sp : role.getPermissions()) {
                        authorities.add(new SimpleGrantedAuthority(sp.name()));
                    }
                }
            }
        }

        // 4. Nạp Quyền Dự Án
        List<ProjectMember> memberships = projectMemberRepository.findByUserId(user.getId());
        if (memberships != null && !memberships.isEmpty()) {
            // Lấy tất cả projectId từ memberships
            List<String> projectIds = memberships.stream()
                    .map(ProjectMember::getProjectId)
                    .toList();

            // Truy vấn tất cả Project trong 1 lần
            List<Project> projects = projectRepository.findAllById(projectIds);
            
            // Map để lookup nhanh
            java.util.Map<String, Project> projectMap = projects.stream()
                    .collect(java.util.stream.Collectors.toMap(Project::getId, p -> p));

            for (ProjectMember member : memberships) {
                Project project = projectMap.get(member.getProjectId());
                if (project != null && project.getCustomRoles() != null) {
                    project.getCustomRoles().stream()
                            .filter(r -> r.getId().equals(member.getRoleId()))
                            .findFirst()
                            .ifPresent(role -> {
                                authorities.add(new SimpleGrantedAuthority(
                                        "PROJECT_" + project.getId() + "_ROLE_" + role.getName().toUpperCase()
                                ));
                                if (role.getPermissions() != null) {
                                    for (Permission p : role.getPermissions()) {
                                        authorities.add(new SimpleGrantedAuthority(
                                                "PROJECT_" + project.getId() + "_" + p.name()
                                        ));
                                    }
                                }
                            });
                }
            }
        }

        MyUserDetails userDetails = MyUserDetails.builder()
                .user(user)
                .authorities(authorities)
                .build();

        // 5. Lưu vào Cache (TTL ví dụ: 30 phút)
        redisService.saveObject(cacheKey, userDetails, 30 * 60 * 1000);

        return userDetails;
    }
}
