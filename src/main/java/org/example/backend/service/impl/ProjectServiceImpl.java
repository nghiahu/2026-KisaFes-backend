package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.AddProjectRequest;
import org.example.backend.dto.request.InviteMemberRequest;
import org.example.backend.dto.response.ProjectResponse;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.example.backend.security.principle.MyUserDetails;
import org.example.backend.service.IProjectService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements IProjectService {

    private final IProjectRepository projectRepository;
    private final IProjectMemberRepository projectMemberRepository;
    private final IUserRepository userRepository;
    private final INotificationRepository notificationRepository;
    private final ISprintRepository sprintRepository;
    private final ITaskRepository taskRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private void broadcastProjectEvent(String projectId, String type) {
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", type);
            payload.put("data", null);
            messagingTemplate.convertAndSend("/topic/project/" + projectId, (Object) payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public ProjectResponse createProject(AddProjectRequest request) {
        // 1. Lấy thông tin người tạo (Owner)
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String ownerId = userDetails.getUserId();

        // 2. Kiểm tra trùng mã dự án
        if (projectRepository.findByCode(request.getCode()).isPresent()) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Mã dự án đã tồn tại");
        }

        // 3. Tạo thực thể Project
        Project project = new Project();
        project.setName(request.getName());
        project.setCode(request.getCode());
        project.setDescription(request.getDescription());
        project.setCategoryId(request.getCategoryId());
        project.setOwnerId(ownerId);
        // Set methodology, default KANBAN if not provided
        project.setMethodology(request.getMethodology() != null
                ? request.getMethodology()
                : Project.Methodology.KANBAN);

        // Map statuses
        if (request.getStatuses() != null) {
            project.setStatuses(request.getStatuses().stream()
                    .map(s -> {
                        Project.ProjectStatus ps = new Project.ProjectStatus();
                        ps.setStatusId(s.getStatusId());
                        ps.setLabel(s.getLabel());
                        ps.setCategory(s.getCategory());
                        ps.setColor(s.getColor());
                        return ps;
                    }).toList());
        }

        // Map board columns
        if (request.getBoardColumns() != null) {
            project.setBoardColumns(request.getBoardColumns().stream()
                    .map(c -> {
                        Project.BoardColumn bc = new Project.BoardColumn();
                        bc.setName(c.getName());
                        bc.setMappedStatusIds(c.getMappedStatusIds());
                        bc.setPosition(c.getPosition());
                        return bc;
                    }).toList());
        }

        // Map roles
        if (request.getRoles() != null) {
            project.setCustomRoles(request.getRoles().stream()
                    .map(r -> {
                        Project.ProjectRole pr = new Project.ProjectRole();
                        pr.setId(java.util.UUID.randomUUID().toString());
                        pr.setName(r.getName());
                        if (r.getPermissions() != null) {
                            pr.setPermissions(r.getPermissions().stream()
                                    .map(p -> {
                                        try {
                                            return Permission.valueOf(p);
                                        } catch (IllegalArgumentException e) {
                                            return null;
                                        }
                                    })
                                    .filter(p -> p != null)
                                    .collect(Collectors.toSet()));
                        }
                        return pr;
                    }).toList());
        }

        // 4. Lưu Project
        Project savedProject = projectRepository.save(project);

        // 5. Tạo ProjectMember cho Owner
        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProjectId(savedProject.getId());
        ownerMember.setUserId(ownerId);
        ownerMember.setJoinedAt(LocalDateTime.now());
        
        if (savedProject.getCustomRoles() != null) {
            savedProject.getCustomRoles().stream()
                    .filter(r -> r.getName().toLowerCase().contains("owner"))
                    .findFirst()
                    .ifPresent(r -> ownerMember.setRoleId(r.getId()));
        }
        projectMemberRepository.save(ownerMember);

        // 6. Xử lý mời Member -> Gửi thông báo lời mời (Notification) thay vì add trực tiếp
        if (request.getMembers() != null) {
            for (String email : request.getMembers()) {
                if (email.equalsIgnoreCase(userDetails.getUser().getEmail())) {
                    throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Bạn không thể tự mời chính mình vào dự án!");
                }

                User recipient = userRepository.findByEmail(email)
                        .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND, "Người dùng với email " + email + " không tồn tại"));

                Notification invitation = Notification.builder()
                        .recipientId(recipient.getId())
                        .senderId(ownerId)
                        .projectId(savedProject.getId())
                        .message(String.format("Bạn đã được mời tham gia dự án '%s' bởi %s.", savedProject.getName(), userDetails.getUsername()))
                        .type(NotificationType.INVITATION)
                        .status(NotificationStatus.PENDING)
                        .build();
                notificationRepository.save(invitation);
            }
        }

        // 7. Tự động seed 1 Sprint kích hoạt và 4 Tasks mẫu cho dự án Scrum mới
        if (savedProject.getStatuses() != null && !savedProject.getStatuses().isEmpty()
                && savedProject.getMethodology() == Project.Methodology.SCRUM) {
            Sprint sprint = new Sprint();
            sprint.setProjectId(savedProject.getId());
            sprint.setName("Sprint 1");
            sprint.setGoal("Khởi chạy dự án và thiết lập ban đầu");
            sprint.setStatus("ACTIVE");
            sprint.setStartDate(LocalDateTime.now());
            sprint.setEndDate(LocalDateTime.now().plusDays(14));
            Sprint savedSprint = sprintRepository.save(sprint);

            String sprintId = savedSprint.getId();
            List<Project.ProjectStatus> statuses = savedProject.getStatuses();
            
            String toDoId = statuses.get(0).getStatusId();
            String inProgressId = statuses.size() > 1 ? statuses.get(1).getStatusId() : toDoId;
            String doneId = statuses.get(statuses.size() - 1).getStatusId();

            Task task1 = new Task();
            task1.setTaskKey(savedProject.getCode() + "-1");
            task1.setProjectId(savedProject.getId());
            task1.setSprintId(sprintId);
            task1.setTitle("Khảo sát yêu cầu & Thiết kế Database");
            task1.setDescription("Làm việc với các bên liên quan để làm rõ sơ đồ cơ sở dữ liệu và yêu cầu chức năng cốt lõi.");
            task1.setStatusId(doneId);
            task1.setPriority("High");
            task1.setStoryPoints(5);
            task1.setAssigneeId(ownerId);
            task1.setReporterId(ownerId);
            task1.setType(TaskType.STORY);
            taskRepository.save(task1);

            Task task2 = new Task();
            task2.setTaskKey(savedProject.getCode() + "-2");
            task2.setProjectId(savedProject.getId());
            task2.setSprintId(sprintId);
            task2.setTitle("Cài đặt khung dự án & Cấu hình Docker");
            task2.setDescription("Khởi tạo mã nguồn cấu trúc thư mục tiêu chuẩn, thiết lập tệp Dockerfile và docker-compose.yml.");
            task2.setStatusId(inProgressId);
            task2.setPriority("Medium");
            task2.setStoryPoints(3);
            task2.setAssigneeId(ownerId);
            task2.setReporterId(ownerId);
            task2.setType(TaskType.STORY);
            taskRepository.save(task2);

            Task task3 = new Task();
            task3.setTaskKey(savedProject.getCode() + "-3");
            task3.setProjectId(savedProject.getId());
            task3.setSprintId(sprintId);
            task3.setTitle("Phát triển mô đun xác thực OAuth2 & Redis");
            task3.setDescription("Xây dựng bộ lọc Spring Security, kết nối Redis cache để tối ưu hóa truy vấn phiên đăng nhập.");
            task3.setStatusId(toDoId);
            task3.setPriority("High");
            task3.setStoryPoints(8);
            task3.setAssigneeId(ownerId);
            task3.setReporterId(ownerId);
            task3.setType(TaskType.STORY);
            taskRepository.save(task3);

            Task task4 = new Task();
            task4.setTaskKey(savedProject.getCode() + "-4");
            task4.setProjectId(savedProject.getId());
            task4.setSprintId(sprintId);
            task4.setTitle("Viết tài liệu hướng dẫn REST API Swagger");
            task4.setDescription("Tích hợp thư viện OpenAPI/Swagger, viết mô tả cho các endpoint đăng ký, đăng nhập và quản lý dự án.");
            task4.setStatusId(toDoId);
            task4.setPriority("Low");
            task4.setStoryPoints(2);
            task4.setAssigneeId(ownerId);
            task4.setReporterId(ownerId);
            task4.setType(TaskType.TASK);
            taskRepository.save(task4);
        }

        ProjectResponse response = ProjectResponse.fromEntity(savedProject);
        populateProjectMetrics(response, savedProject);
        return response;
    }

    @Override
    public List<ProjectResponse> getAllProjects() {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = userDetails.getUserId();

        List<ProjectMember> memberships = projectMemberRepository.findByUserId(userId);
        List<String> projectIds = memberships.stream().map(ProjectMember::getProjectId).toList();

        List<Project> projects = projectRepository.findAllById(projectIds);

        return projects.stream().map(project -> {
            ProjectResponse response = ProjectResponse.fromEntity(project);
            List<ProjectMember> members = projectMemberRepository.findByProjectId(project.getId());
            List<String> memberUserIds = members.stream().map(ProjectMember::getUserId).toList();
            
            List<User> users = userRepository.findAllById(memberUserIds);
            
            response.setMembers(members.stream().map(m -> {
                ProjectResponse.MemberResponse mr = new ProjectResponse.MemberResponse();
                mr.setId(m.getUserId());
                mr.setActive(m.isActive());
                users.stream().filter(u -> u.getId().equals(m.getUserId())).findFirst()
                        .ifPresent(u -> {
                            mr.setName(u.getFullName());
                            mr.setAvatar(u.getAvatar());
                        });
                mr.setRoleId(m.getRoleId());
                if (project.getCustomRoles() != null) {
                    project.getCustomRoles().stream()
                            .filter(r -> r.getId().equals(m.getRoleId()))
                            .findFirst()
                            .ifPresent(r -> mr.setRoleName(r.getName()));
                }
                return mr;
            }).toList());
            
            populateProjectMetrics(response, project);
            return response;
        }).toList();
    }

    @Override
    public ProjectResponse getProjectById(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        ProjectResponse response = ProjectResponse.fromEntity(project);
        
        List<ProjectMember> members = projectMemberRepository.findByProjectId(project.getId());
        List<String> memberUserIds = members.stream().map(ProjectMember::getUserId).toList();
        List<User> users = userRepository.findAllById(memberUserIds);
        
        response.setMembers(members.stream().map(m -> {
            ProjectResponse.MemberResponse mr = new ProjectResponse.MemberResponse();
            mr.setId(m.getUserId());
            mr.setActive(m.isActive());
            users.stream().filter(u -> u.getId().equals(m.getUserId())).findFirst()
                    .ifPresent(u -> {
                        mr.setName(u.getFullName());
                        mr.setAvatar(u.getAvatar());
                    });
            mr.setRoleId(m.getRoleId());
            if (project.getCustomRoles() != null) {
                project.getCustomRoles().stream()
                        .filter(r -> r.getId().equals(m.getRoleId()))
                        .findFirst()
                        .ifPresent(r -> mr.setRoleName(r.getName()));
            }
            return mr;
        }).toList());

        populateProjectMetrics(response, project);
        return response;
    }

    private void populateProjectMetrics(ProjectResponse response, Project project) {
        if (project == null || response == null) return;

        sprintRepository.findFirstByProjectIdAndStatus(project.getId(), "ACTIVE")
                .ifPresentOrElse(
                        s -> response.setActiveSprintName(s.getName()),
                        () -> response.setActiveSprintName("No active sprint")
                );

        List<Task> tasks = taskRepository.findByProjectId(project.getId());
        response.setTotalTasksCount(tasks.size());

        if (!tasks.isEmpty() && project.getStatuses() != null) {
            List<String> doneStatusIds = project.getStatuses().stream()
                    .filter(s -> s.getCategory() == StatusCategory.DONE)
                    .map(Project.ProjectStatus::getStatusId)
                    .toList();

            int completedCount = (int) tasks.stream()
                    .filter(t -> doneStatusIds.contains(t.getStatusId()))
                    .count();
            response.setCompletedTasksCount(completedCount);

            int blockedCount = (int) tasks.stream()
                    .filter(t -> "High".equalsIgnoreCase(t.getPriority()) && !doneStatusIds.contains(t.getStatusId()))
                    .count();
            response.setBlockedTasksCount(blockedCount);

            response.setOpenIssuesCount(tasks.size() - completedCount);
        } else {
            response.setCompletedTasksCount(0);
            response.setBlockedTasksCount(0);
            response.setOpenIssuesCount(0);
        }

        response.setDeadlineDisplay("Ends in 5 days");
        sprintRepository.findFirstByProjectIdAndStatus(project.getId(), "ACTIVE")
                .ifPresent(s -> {
                    if (s.getEndDate() != null) {
                        java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), s.getEndDate());
                        long days = duration.toDays();
                        if (days > 0) {
                            response.setDeadlineDisplay("Ends in " + days + " days");
                        } else if (days == 0) {
                            response.setDeadlineDisplay("Ends today");
                        } else {
                            response.setDeadlineDisplay("Sprint ended");
                        }
                    }
                });
    }

    @Override
    @Transactional
    public void inviteMember(String projectId, InviteMemberRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.MEMBER_INVITE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền mời thành viên vào dự án này");
        }

        if (request.getEmail().equalsIgnoreCase(userDetails.getUser().getEmail())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Bạn không thể tự mời chính mình vào dự án!");
        }

        User recipient = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND, "Người dùng với email " + request.getEmail() + " không tồn tại"));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, recipient.getId())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Người dùng này đã là thành viên của dự án");
        }

        Notification invitation = Notification.builder()
                .recipientId(recipient.getId())
                .senderId(userDetails.getUserId())
                .projectId(projectId)
                .message(String.format("Bạn đã được mời tham gia dự án '%s' bởi %s.", project.getName(), userDetails.getUsername()))
                .type(NotificationType.INVITATION)
                .status(NotificationStatus.PENDING)
                .build();
        
        notificationRepository.save(invitation);
    }

    @Override
    @Transactional
    public void removeMember(String projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.MEMBER_REMOVE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền xóa thành viên khỏi dự án này");
        }

        if (project.getOwnerId().equals(userId)) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Không thể xóa chủ sở hữu khỏi dự án");
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Thành viên không tồn tại trong dự án"));

        member.setActive(false);
        member.setRemovedAt(LocalDateTime.now());
        projectMemberRepository.save(member);

        // Unassign tasks
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        boolean changed = false;
        for (Task task : tasks) {
            if (userId.equals(task.getAssigneeId())) {
                task.setAssigneeId(null);
                changed = true;
            }
        }
        if (changed) {
            taskRepository.saveAll(tasks);
        }
        broadcastProjectEvent(projectId, "UPDATE_PROJECT");
    }

    @Override
    @Transactional
    public void restoreMember(String projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.MEMBER_INVITE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền mời/khôi phục thành viên trong dự án này");
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Thành viên không tồn tại trong dự án"));

        if (member.isActive()) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Thành viên này vẫn đang hoạt động");
        }

        member.setActive(true);
        member.setRemovedAt(null);
        projectMemberRepository.save(member);
        broadcastProjectEvent(projectId, "UPDATE_PROJECT");
    }

    @Override
    @Transactional
    public void changeMemberRole(String projectId, String userId, org.example.backend.dto.request.ChangeRoleRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.MEMBER_UPDATE_ROLE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền thay đổi quyền thành viên trong dự án này");
        }

        if (project.getOwnerId().equals(userId)) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Không thể thay đổi quyền của chủ sở hữu");
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Thành viên không tồn tại trong dự án"));

        boolean roleExists = false;
        if (project.getCustomRoles() != null) {
            roleExists = project.getCustomRoles().stream().anyMatch(r -> r.getId().equals(request.getRoleId()));
        }
        if (!roleExists) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Role không tồn tại trong dự án");
        }

        member.setRoleId(request.getRoleId());
        projectMemberRepository.save(member);
        broadcastProjectEvent(projectId, "UPDATE_PROJECT");
    }

    @Override
    @Transactional
    public Project.ProjectRole addCustomRole(String projectId, org.example.backend.dto.request.AddProjectRoleRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.ROLE_MANAGE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền quản lý vai trò trong dự án này");
        }

        if (project.getCustomRoles() == null) {
            project.setCustomRoles(new ArrayList<>());
        }

        boolean nameExists = project.getCustomRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(request.getName()));
        if (nameExists) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Tên Role đã tồn tại");
        }

        java.util.Set<Permission> permissions = new java.util.HashSet<>();
        if (request.getPermissions() != null) {
            for (String p : request.getPermissions()) {
                try {
                    permissions.add(Permission.valueOf(p));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid
                }
            }
        }

        Project.ProjectRole newRole = Project.ProjectRole.builder()
                .id(java.util.UUID.randomUUID().toString())
                .name(request.getName())
                .permissions(permissions)
                .build();

        project.getCustomRoles().add(newRole);
        projectRepository.save(project);
        broadcastProjectEvent(projectId, "UPDATE_PROJECT");

        return newRole;
    }

    @Override
    @Transactional
    public Project.ProjectRole updateCustomRole(String projectId, String roleId, org.example.backend.dto.request.UpdateProjectRoleRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.ROLE_MANAGE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền quản lý vai trò trong dự án này");
        }

        if (project.getCustomRoles() == null) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy vai trò cần sửa");
        }

        Project.ProjectRole targetRole = project.getCustomRoles().stream()
                .filter(r -> r.getId().equals(roleId))
                .findFirst()
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Vai trò không tồn tại trong dự án"));

        // Do not allow editing owner role
        if (targetRole.getName().toLowerCase().contains("owner")) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Không thể chỉnh sửa quyền của vai trò Owner (Chủ sở hữu)");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(targetRole.getName())) {
                boolean nameExists = project.getCustomRoles().stream()
                        .anyMatch(r -> r.getName().equalsIgnoreCase(newName));
                if (nameExists) {
                    throw new CustomBusinessException(ErrorCode.RESOURCE_ALREADY_EXISTS, "Tên Role đã tồn tại");
                }
                targetRole.setName(newName);
            }
        }

        if (request.getPermissions() != null) {
            java.util.Set<Permission> permissions = new java.util.HashSet<>();
            for (String p : request.getPermissions()) {
                try {
                    permissions.add(Permission.valueOf(p));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid
                }
            }
            targetRole.setPermissions(permissions);
        }

        projectRepository.save(project);
        broadcastProjectEvent(projectId, "UPDATE_PROJECT");
        return targetRole;
    }

    @Override
    public boolean hasPermission(String projectId, String userId, Permission permission) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return false;

        // Owner/Creator automatically has all permissions
        if (userId.equals(project.getOwnerId())) {
            return true;
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId).orElse(null);
        if (member == null || !member.isActive()) {
            return false;
        }

        String targetRoleId = member.getRoleId();

        // Fallback: If member's roleId is null, resolve it to the first 'developer' or 'member' role in the project
        if (targetRoleId == null && project.getCustomRoles() != null) {
            targetRoleId = project.getCustomRoles().stream()
                    .filter(r -> r.getName().toLowerCase().contains("developer") || r.getName().toLowerCase().contains("member"))
                    .map(Project.ProjectRole::getId)
                    .findFirst()
                    .orElse(null);
        }

        if (project.getCustomRoles() != null && targetRoleId != null) {
            for (Project.ProjectRole role : project.getCustomRoles()) {
                if (targetRoleId.equals(role.getId())) {
                    return role.getPermissions() != null && role.getPermissions().contains(permission);
                }
            }
        }

        return false;
    }

    @Override
    @Transactional
    public ProjectResponse updateProjectName(String projectId, org.example.backend.dto.request.UpdateProjectNameRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.PROJECT_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền chỉnh sửa tên dự án này");
        }

        project.setName(request.getName().trim());
        projectRepository.save(project);
        broadcastProjectEvent(projectId, "UPDATE_PROJECT");

        ProjectResponse response = ProjectResponse.fromEntity(project);
        populateProjectMetrics(response, project);
        return response;
    }
}

