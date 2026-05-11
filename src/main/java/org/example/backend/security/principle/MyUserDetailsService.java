package org.example.backend.security.principle;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.ProjectMember;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.repository.IProjectMemberRepository;
import org.example.backend.repository.IUserRepository;
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

    @Override
    @Transactional(readOnly = true) // Đảm bảo hiệu suất truy vấn MongoDB
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 1. Tìm user bằng username hoặc email (Đã khớp với Entity User của bạn)
        User user = userRepository.findByUserNameOrEmail(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + identifier));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 2. Nạp Quyền Hệ Thống (Ví dụ: ROLE_ADMIN, ROLE_USER)
        if (user.getRoles() != null) {
            user.getRoles().stream()
                    .map(Role::getName)
                    .filter(roleName -> roleName != null && !roleName.isBlank())
                    .forEach(roleName ->
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()))
                    );
        }

        // 3. Nạp Quyền Dự Án (Dựa trên Entity ProjectMember)
        List<ProjectMember> memberships = projectMemberRepository.findByUserId(user.getId());
        if (memberships != null) {
            memberships.forEach(member -> {
                authorities.add(new SimpleGrantedAuthority(
                        "PROJECT_" + member.getProjectId() + "_" + member.getRoleId().toUpperCase()
                ));
            });
        }

        // 4. Trả về MyUserDetails (Đảm bảo MyUserDetails của bạn có field chứa memberships nếu cần dùng sau này)
        return MyUserDetails.builder()
                .user(user)
                .authorities(authorities)
                // .projectMemberships(memberships)
                .build();
    }
}