package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.AddProjectRequest;
import org.example.backend.dto.request.AddProjectRoleRequest;
import org.example.backend.dto.request.ChangeRoleRequest;
import org.example.backend.dto.request.InviteMemberRequest;
import org.example.backend.dto.response.ProjectResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.entity.Project;
import org.example.backend.service.IProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

import org.example.backend.dto.request.UpdateProjectRoleRequest;

import org.springframework.web.bind.annotation.PatchMapping;
import org.example.backend.dto.request.UpdateProjectNameRequest;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController extends BaseController {

    private final IProjectService projectService;

    @PostMapping
    public ResponseEntity<ResponseWrapper<ProjectResponse>> createProject(@Valid @RequestBody AddProjectRequest request) {
        return created(projectService.createProject(request), "Tạo dự án thành công");
    }

    @GetMapping
    public ResponseEntity<ResponseWrapper<List<ProjectResponse>>> getAllProjects() {
        return success(projectService.getAllProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseWrapper<ProjectResponse>> getProjectById(@PathVariable String id) {
        return success(projectService.getProjectById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseWrapper<Void>> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return success(null, "Xóa dự án thành công");
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<ResponseWrapper<ProjectResponse>> updateProjectName(
            @PathVariable String id,
            @Valid @RequestBody UpdateProjectNameRequest request) {
        return success(projectService.updateProjectName(id, request), "Đã cập nhật tên dự án");
    }

    @PutMapping("/{id}/info")
    public ResponseEntity<ResponseWrapper<ProjectResponse>> updateProjectInfo(
            @PathVariable String id,
            @Valid @RequestBody org.example.backend.dto.request.UpdateProjectInfoRequest request) {
        return success(projectService.updateProjectInfo(id, request), "Đã cập nhật thông tin dự án");
    }

    @PostMapping("/{id}/teams")
    public ResponseEntity<ResponseWrapper<ProjectResponse>> addTeamToProject(
            @PathVariable String id,
            @RequestParam String teamId,
            @RequestParam(required = false) String roleId) {
        return success(projectService.addTeamToProject(id, teamId, roleId), "Thêm nhóm vào dự án thành công");
    }

    @DeleteMapping("/{id}/teams/{teamId}")
    public ResponseEntity<ResponseWrapper<Void>> removeTeamFromProject(
            @PathVariable String id,
            @PathVariable String teamId) {
        projectService.removeTeamFromProject(id, teamId);
        return success(null, "Xóa nhóm khỏi dự án thành công");
    }

    @GetMapping("/{id}/teams")
    public ResponseEntity<ResponseWrapper<List<org.example.backend.dto.response.TeamResponse>>> getProjectTeams(@PathVariable String id) {
        return success(projectService.getProjectTeams(id), "Lấy danh sách nhóm thành công");
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<ResponseWrapper<Void>> inviteMember(
            @PathVariable String id,
            @Valid @RequestBody InviteMemberRequest request) {
        projectService.inviteMember(id, request);
        return success(null, "Đã gửi lời mời thành công");
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ResponseWrapper<Void>> removeMember(
            @PathVariable String id, 
            @PathVariable String userId) {
        projectService.removeMember(id, userId);
        return success(null, "Đã xóa thành viên khỏi dự án");
    }

    @PutMapping("/{id}/members/{userId}/restore")
    public ResponseEntity<ResponseWrapper<Void>> restoreMember(
            @PathVariable String id, 
            @PathVariable String userId) {
        projectService.restoreMember(id, userId);
        return success(null, "Đã khôi phục thành viên vào dự án");
    }

    @PutMapping("/{id}/members/{userId}/role")
    public ResponseEntity<ResponseWrapper<Void>> changeMemberRole(
            @PathVariable String id,
            @PathVariable String userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        projectService.changeMemberRole(id, userId, request);
        return success(null, "Đã cập nhật quyền thành viên");
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<ResponseWrapper<Project.ProjectRole>> addCustomRole(
            @PathVariable String id,
            @Valid @RequestBody AddProjectRoleRequest request) {
        return success(projectService.addCustomRole(id, request), "Đã tạo role thành công");
    }

    @PutMapping("/{id}/roles/{roleId}")
    public ResponseEntity<ResponseWrapper<Project.ProjectRole>> updateCustomRole(
            @PathVariable String id,
            @PathVariable String roleId,
            @Valid @RequestBody UpdateProjectRoleRequest request) {
        return success(projectService.updateCustomRole(id, roleId, request), "Cập nhật vai trò thành công");
    }
}

