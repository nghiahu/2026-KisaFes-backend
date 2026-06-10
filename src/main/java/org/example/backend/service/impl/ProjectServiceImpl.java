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
import org.springframework.context.ApplicationEventPublisher;
import org.example.backend.event.NotificationEvent;
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
    private final ApplicationEventPublisher eventPublisher;
    private final ISprintRepository sprintRepository;
    private final ITaskRepository taskRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final ITeamRepository teamRepository;
    private final ITaskActivityRepository taskActivityRepository;
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

                NotificationEvent invitationEvent = new NotificationEvent(this,
                        recipient.getId(),
                        ownerId,
                        savedProject.getId(),
                        String.format("Bạn đã được mời tham gia dự án '%s' bởi %s.", savedProject.getName(), userDetails.getUsername()),
                        NotificationType.INVITATION,
                        NotificationStatus.PENDING);
                eventPublisher.publishEvent(invitationEvent);
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

        }

        ProjectResponse response = ProjectResponse.fromEntity(savedProject);
        populateProjectMetrics(response, savedProject);
        return response;
    }

    @Override
    public List<ProjectResponse> getAllProjects() {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = userDetails.getUserId();

        // 1. Lấy các dự án user là thành viên trực tiếp
        List<ProjectMember> memberships = projectMemberRepository.findByUserId(userId);
        List<String> projectIds = memberships.stream().map(ProjectMember::getProjectId).toList();
        java.util.List<Project> projects = new java.util.ArrayList<>(projectRepository.findAllById(projectIds));

        // 2. Lấy các dự án user là thành viên qua nhóm
        List<TeamMember> userTeams = teamMemberRepository.findByUserId(userId);
        if (!userTeams.isEmpty()) {
            List<String> teamIds = userTeams.stream().map(TeamMember::getTeamId).toList();
            List<Project> teamProjects = projectRepository.findProjectsByTeamIds(teamIds);
            
            for (Project tp : teamProjects) {
                if (projects.stream().noneMatch(p -> p.getId().equals(tp.getId()))) {
                    projects.add(tp);
                }
            }
        }

        return projects.stream().map(project -> {
            ProjectResponse response = ProjectResponse.fromEntity(project);
            response.setMembers(getProjectMembers(project));
            populateProjectMetrics(response, project);
            return response;
        }).toList();
    }

    @Override
    public ProjectResponse getProjectById(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        ProjectResponse response = ProjectResponse.fromEntity(project);
        
        response.setMembers(getProjectMembers(project));

        populateProjectMetrics(response, project);
        return response;
    }

    @Override
    public List<org.example.backend.dto.response.RecentActivityDto> getRecentActivities(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        List<TaskActivity> activities = taskActivityRepository.findTop10ByProjectIdOrderByCreatedAtDesc(projectId);

        List<String> taskIds = activities.stream().map(TaskActivity::getTaskId).distinct().toList();
        List<String> userIds = activities.stream().map(TaskActivity::getUserId).distinct().toList();

        List<Task> tasks = taskRepository.findAllById(taskIds);
        List<User> users = userRepository.findAllById(userIds);

        java.util.Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        java.util.Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        return activities.stream().map(a -> {
            org.example.backend.dto.response.RecentActivityDto dto = new org.example.backend.dto.response.RecentActivityDto();
            dto.setId(a.getId());
            dto.setActionType(a.getActionType());
            dto.setField(a.getField());
            dto.setOldValue(a.getOldValue());
            dto.setNewValue(a.getNewValue());
            dto.setCreatedAt(a.getCreatedAt());

            User user = userMap.get(a.getUserId());
            if (user != null) {
                dto.setUserId(user.getId());
                dto.setUserName(user.getFullName());
                dto.setUserAvatar(user.getAvatar());
            }

            Task task = taskMap.get(a.getTaskId());
            if (task != null) {
                dto.setTaskId(task.getId());
                dto.setTaskKey(task.getTaskKey() != null ? task.getTaskKey() : task.getId());
                dto.setTaskTitle(task.getTitle());
                dto.setTaskType(task.getType() != null ? task.getType().name() : null);
                dto.setTaskStatusId(task.getStatusId());
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProject(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.PROJECT_DELETE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền xóa dự án này");
        }

        // Clean up tasks
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        if (!tasks.isEmpty()) {
            taskRepository.deleteAll(tasks);
        }

        // Clean up sprints
        List<Sprint> sprints = sprintRepository.findByProjectId(projectId);
        if (!sprints.isEmpty()) {
            sprintRepository.deleteAll(sprints);
        }

        // Clean up members
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        if (!members.isEmpty()) {
            projectMemberRepository.deleteAll(members);
        }

        projectRepository.deleteById(projectId);
        broadcastProjectEvent(projectId, "DELETE_PROJECT");
    }

    private List<ProjectResponse.MemberResponse> getProjectMembers(Project project) {
        List<ProjectMember> explicitMembers = projectMemberRepository.findByProjectId(project.getId());
        java.util.Map<String, ProjectResponse.MemberResponse> memberMap = new java.util.HashMap<>();
        
        if (!explicitMembers.isEmpty()) {
            List<String> explicitUserIds = explicitMembers.stream().map(ProjectMember::getUserId).toList();
            List<User> explicitUsers = userRepository.findAllById(explicitUserIds);
            
            for (ProjectMember m : explicitMembers) {
                ProjectResponse.MemberResponse mr = new ProjectResponse.MemberResponse();
                mr.setId(m.getUserId());
                mr.setActive(m.isActive());
                mr.setRoleId(m.getRoleId());
                
                explicitUsers.stream().filter(u -> u.getId().equals(m.getUserId())).findFirst()
                        .ifPresent(u -> {
                            mr.setName(u.getFullName());
                            mr.setAvatar(u.getAvatar());
                        });
                        
                if (project.getCustomRoles() != null) {
                    project.getCustomRoles().stream()
                            .filter(r -> r.getId().equals(m.getRoleId()))
                            .findFirst()
                            .ifPresent(r -> mr.setRoleName(r.getName()));
                }
                memberMap.put(m.getUserId(), mr);
            }
        }
        
        if (project.getTeams() != null && !project.getTeams().isEmpty()) {
            for (Project.ProjectTeam pt : project.getTeams()) {
                List<TeamMember> teamMembers = teamMemberRepository.findByTeamId(pt.getTeamId());
                if (!teamMembers.isEmpty()) {
                    List<String> teamUserIds = teamMembers.stream().map(TeamMember::getUserId).toList();
                    List<User> teamUsers = userRepository.findAllById(teamUserIds);
                    
                    for (TeamMember tm : teamMembers) {
                        if (!memberMap.containsKey(tm.getUserId())) {
                            ProjectResponse.MemberResponse mr = new ProjectResponse.MemberResponse();
                            mr.setId(tm.getUserId());
                            mr.setActive(true);
                            mr.setRoleId(pt.getRoleId());
                            
                            teamUsers.stream().filter(u -> u.getId().equals(tm.getUserId())).findFirst()
                                    .ifPresent(u -> {
                                        mr.setName(u.getFullName());
                                        mr.setAvatar(u.getAvatar());
                                    });
                            
                            if (project.getCustomRoles() != null) {
                                project.getCustomRoles().stream()
                                        .filter(r -> r.getId().equals(pt.getRoleId()))
                                        .findFirst()
                                        .ifPresent(r -> mr.setRoleName(r.getName() + " (Từ nhóm)"));
                            }
                            
                            memberMap.put(tm.getUserId(), mr);
                        }
                    }
                }
            }
        }
        
        return new ArrayList<>(memberMap.values());
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

        NotificationEvent invitationEvent = new NotificationEvent(this,
                recipient.getId(),
                userDetails.getUserId(),
                projectId,
                String.format("Bạn đã được mời tham gia dự án '%s' bởi %s.", project.getName(), userDetails.getUsername()),
                NotificationType.INVITATION,
                NotificationStatus.PENDING);
        
        eventPublisher.publishEvent(invitationEvent);
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

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId).orElse(null);
        if (member == null) {
            if (project.getTeams() != null && !project.getTeams().isEmpty()) {
                List<TeamMember> userTeams = teamMemberRepository.findByUserId(userId);
                for (Project.ProjectTeam pt : project.getTeams()) {
                    if (userTeams.stream().anyMatch(t -> t.getTeamId().equals(pt.getTeamId()))) {
                        throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Người dùng này thuộc một nhóm được phân công vào dự án. Bạn phải xóa họ ở mức độ Nhóm thay vì cá nhân.");
                    }
                }
            }
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Thành viên không tồn tại trong dự án");
        }

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

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId).orElse(null);
        if (member == null) {
            boolean isTeamMember = false;
            if (project.getTeams() != null && !project.getTeams().isEmpty()) {
                List<TeamMember> userTeams = teamMemberRepository.findByUserId(userId);
                for (Project.ProjectTeam pt : project.getTeams()) {
                    if (userTeams.stream().anyMatch(t -> t.getTeamId().equals(pt.getTeamId()))) {
                        isTeamMember = true;
                        break;
                    }
                }
            }
            if (isTeamMember) {
                member = new ProjectMember();
                member.setProjectId(projectId);
                member.setUserId(userId);
                member.setJoinedAt(LocalDateTime.now());
                member.setActive(true);
            } else {
                throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Thành viên không tồn tại trong dự án");
            }
        }

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
        String targetRoleId = null;
        if (member != null && member.isActive()) {
            targetRoleId = member.getRoleId();
        }
        
        // If not found in individual members, check if user is in any team assigned to the project
        if (targetRoleId == null && project.getTeams() != null && !project.getTeams().isEmpty()) {
            List<TeamMember> userTeams = teamMemberRepository.findByUserId(userId);
            for (Project.ProjectTeam pt : project.getTeams()) {
                if (userTeams.stream().anyMatch(t -> t.getTeamId().equals(pt.getTeamId()))) {
                    targetRoleId = pt.getRoleId();
                    break;
                }
            }
        }

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

    @Override
    @Transactional
    public ProjectResponse updateProjectInfo(String projectId, org.example.backend.dto.request.UpdateProjectInfoRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.PROJECT_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền chỉnh sửa dự án này");
        }

        project.setName(request.getName().trim());
        project.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        project.setCategoryId(request.getCategoryId());

        projectRepository.save(project);
        broadcastProjectEvent(projectId, "UPDATE_PROJECT");

        ProjectResponse response = ProjectResponse.fromEntity(project);
        populateProjectMetrics(response, project);
        return response;
    }

    @Transactional
    public ProjectResponse addTeamToProject(String projectId, String teamId, String roleId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.MEMBER_INVITE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền mời thành viên/nhóm vào dự án này");
        }

        if (project.getTeams() == null) {
            project.setTeams(new ArrayList<>());
        }

        if (project.getTeams().stream().anyMatch(t -> t.getTeamId().equals(teamId))) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Nhóm này đã được gán vào dự án");
        }

        Project.ProjectTeam pt = new Project.ProjectTeam();
        pt.setTeamId(teamId);
        pt.setRoleId(roleId);
        pt.setAssignedAt(LocalDateTime.now());
        
        project.getTeams().add(pt);
        projectRepository.save(project);
        broadcastProjectEvent(projectId, "UPDATE_PROJECT");
        
        ProjectResponse response = ProjectResponse.fromEntity(project);
        populateProjectMetrics(response, project);
        return response;
    }

    @Transactional
    public void removeTeamFromProject(String projectId, String teamId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!hasPermission(projectId, userDetails.getUserId(), Permission.MEMBER_REMOVE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền xóa thành viên/nhóm khỏi dự án này");
        }

        if (project.getTeams() != null) {
            boolean removed = project.getTeams().removeIf(t -> t.getTeamId().equals(teamId));
            if (removed) {
                projectRepository.save(project);
                broadcastProjectEvent(projectId, "UPDATE_PROJECT");
            }
        }
    }

    @Override
    public List<org.example.backend.dto.response.TeamResponse> getProjectTeams(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        if (project.getTeams() == null || project.getTeams().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> teamIds = project.getTeams().stream().map(Project.ProjectTeam::getTeamId).toList();
        List<Team> teams = teamRepository.findAllById(teamIds);

        return teams.stream()
                .map(t -> org.example.backend.dto.response.TeamResponse.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .description(t.getDescription())
                        .avatar(t.getAvatar())
                        .coverImage(t.getCoverImage())
                        .build())
                .toList();
    }
}

