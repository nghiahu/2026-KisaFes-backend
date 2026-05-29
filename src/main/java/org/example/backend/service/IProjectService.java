package org.example.backend.service;
import org.example.backend.dto.request.AddProjectRequest;
import org.example.backend.dto.request.AddProjectRoleRequest;
import org.example.backend.dto.request.ChangeRoleRequest;
import org.example.backend.dto.request.InviteMemberRequest;
import org.example.backend.dto.response.ProjectResponse;
import org.example.backend.entity.Project;

import java.util.List;

public interface IProjectService {
    ProjectResponse createProject(AddProjectRequest addProjectRequest);
    List<ProjectResponse> getAllProjects();
    ProjectResponse getProjectById(String id);
    void inviteMember(String projectId, InviteMemberRequest request);
    void removeMember(String projectId, String userId);
    void restoreMember(String projectId, String userId);
    void changeMemberRole(String projectId, String userId, ChangeRoleRequest request);
    Project.ProjectRole addCustomRole(String projectId, AddProjectRoleRequest request);
    Project.ProjectRole updateCustomRole(String projectId, String roleId, org.example.backend.dto.request.UpdateProjectRoleRequest request);
    boolean hasPermission(String projectId, String userId, org.example.backend.entity.Permission permission);
    ProjectResponse updateProjectName(String projectId, org.example.backend.dto.request.UpdateProjectNameRequest request);
    ProjectResponse updateProjectInfo(String projectId, org.example.backend.dto.request.UpdateProjectInfoRequest request);
}
