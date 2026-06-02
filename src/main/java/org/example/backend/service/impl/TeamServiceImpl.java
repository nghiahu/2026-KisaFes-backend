package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.TeamRequest;
import org.example.backend.dto.response.TeamResponse;
import org.example.backend.entity.Team;
import org.example.backend.entity.TeamMember;
import org.example.backend.entity.User;
import org.example.backend.repository.ITeamMemberRepository;
import org.example.backend.repository.ITeamRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.security.principle.MyUserDetails;
import org.example.backend.service.ITeamService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements ITeamService {

    private final ITeamRepository teamRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final IUserRepository userRepository;
    private final org.example.backend.repository.IProjectRepository projectRepository;
    private final org.example.backend.repository.ITaskRepository taskRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    private void broadcastProjectUpdate(String teamId) {
        try {
            java.util.List<org.example.backend.entity.Project> projects = projectRepository.findProjectsByTeamIds(java.util.Collections.singletonList(teamId));
            for (org.example.backend.entity.Project p : projects) {
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("type", "UPDATE_PROJECT");
                payload.put("data", null);
                messagingTemplate.convertAndSend("/topic/project/" + p.getId(), (Object) payload);
            }
            
            java.util.Map<String, Object> teamPayload = new java.util.HashMap<>();
            teamPayload.put("type", "UPDATE_TEAM");
            teamPayload.put("data", null);
            messagingTemplate.convertAndSend("/topic/team/" + teamId, (Object) teamPayload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setAvatar(request.getAvatar());
        team.setCoverImage(request.getCoverImage());
        team = teamRepository.save(team);
        
        TeamMember owner = new TeamMember();
        owner.setTeamId(team.getId());
        owner.setUserId(userDetails.getUserId());
        owner.setRole("ADMIN");
        owner.setJoinedAt(LocalDateTime.now());
        teamMemberRepository.save(owner);
        
        return buildTeamResponse(team);
    }

    @Override
    public TeamResponse getTeamById(String id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Team not found"));
        return buildTeamResponse(team);
    }

    @Override
    @Transactional
    public TeamResponse updateTeam(String id, TeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Team not found"));
        
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        if (request.getAvatar() != null) {
            team.setAvatar(request.getAvatar());
        }
        if (request.getCoverImage() != null) {
            team.setCoverImage(request.getCoverImage());
        }
        
        team = teamRepository.save(team);
        return buildTeamResponse(team);
    }

    @Override
    public List<TeamResponse> searchTeams(String keyword) {
        List<Team> teams;
        if (keyword == null || keyword.trim().isEmpty()) {
            teams = teamRepository.findAll();
        } else {
            teams = teamRepository.findByNameContainingIgnoreCase(keyword.trim());
        }
        return teams.stream().map(this::buildTeamResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeamResponse addMemberToTeam(String teamId, String email, String role) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Team not found"));
                
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
                
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, user.getId())) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "User is already in the team");
        }
        
        org.example.backend.security.principle.MyUserDetails userDetails = (org.example.backend.security.principle.MyUserDetails) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        org.example.backend.event.NotificationEvent invitationEvent = new org.example.backend.event.NotificationEvent(
                this,
                user.getId(),
                userDetails.getUserId(),
                null,
                teamId,
                String.format("Bạn đã được mời tham gia nhóm '%s' bởi %s.", team.getName(), userDetails.getUsername()),
                org.example.backend.entity.NotificationType.INVITATION,
                org.example.backend.entity.NotificationStatus.PENDING
        );
        
        eventPublisher.publishEvent(invitationEvent);
        
        return buildTeamResponse(team);
    }

    @Override
    @Transactional
    public void removeMemberFromTeam(String teamId, String userId) {
        TeamMember member = teamMemberRepository.findByTeamId(teamId).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Member not found in team"));
                
        teamMemberRepository.delete(member);
        
        broadcastProjectUpdate(teamId);
    }

    @Override
    public List<org.example.backend.dto.response.ProjectResponse> getProjectsByTeam(String teamId) {
        List<org.example.backend.entity.Project> projects = projectRepository.findProjectsByTeamIds(java.util.Collections.singletonList(teamId));
        return projects.stream()
                .map(p -> {
                    org.example.backend.dto.response.ProjectResponse res = new org.example.backend.dto.response.ProjectResponse();
                    res.setId(p.getId());
                    res.setName(p.getName());
                    res.setCode(p.getCode());
                    res.setDescription(p.getDescription());
                    // add other basic fields if necessary, or just basic mapping
                    return res;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<org.example.backend.dto.response.TaskResponse> getTasksByTeam(String teamId) {
        List<org.example.backend.entity.Task> tasks = taskRepository.findByTeamId(teamId);
        
        List<String> projectIds = tasks.stream().map(org.example.backend.entity.Task::getProjectId).distinct().collect(Collectors.toList());
        java.util.Map<String, org.example.backend.entity.Project> projectMap = projectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(org.example.backend.entity.Project::getId, p -> p));
                
        List<String> userIds = tasks.stream().map(org.example.backend.entity.Task::getAssigneeId).filter(id -> id != null).distinct().collect(Collectors.toList());
        java.util.Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return tasks.stream()
                .map(t -> {
                    org.example.backend.dto.response.TaskResponse res = new org.example.backend.dto.response.TaskResponse();
                    res.setId(t.getId());
                    res.setTaskKey(t.getTaskKey());
                    res.setTitle(t.getTitle());
                    res.setStatusId(t.getStatusId());
                    res.setPriority(t.getPriority());
                    res.setProjectId(t.getProjectId());
                    
                    org.example.backend.entity.Project p = projectMap.get(t.getProjectId());
                    if (p != null) {
                        res.setProjectName(p.getName());
                    }
                    
                    if (t.getAssigneeId() != null) {
                        User u = userMap.get(t.getAssigneeId());
                        if (u != null) {
                            res.setAssigneeId(u.getId());
                            res.setAssigneeName(u.getFullName());
                            res.setAssigneeAvatar(u.getAvatar());
                        }
                    }
                    return res;
                })
                .collect(Collectors.toList());
    }

    private TeamResponse buildTeamResponse(Team team) {
        List<TeamMember> members = teamMemberRepository.findByTeamId(team.getId());
        List<String> userIds = members.stream().map(TeamMember::getUserId).collect(Collectors.toList());
        List<User> users = (List<User>) userRepository.findAllById(userIds);
        
        List<TeamResponse.MemberInfo> memberInfos = new ArrayList<>();
        for (TeamMember m : members) {
            users.stream().filter(u -> u.getId().equals(m.getUserId())).findFirst().ifPresent(u -> {
                memberInfos.add(TeamResponse.MemberInfo.builder()
                        .id(u.getId())
                        .name(u.getFullName())
                        .email(u.getEmail())
                        .avatar(u.getAvatar())
                        .role(m.getRole())
                        .joinedAt(m.getJoinedAt() != null ? m.getJoinedAt().toString() : null)
                        .build());
            });
        }
        
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .avatar(team.getAvatar())
                .coverImage(team.getCoverImage())
                .members(memberInfos)
                .build();
    }
}
