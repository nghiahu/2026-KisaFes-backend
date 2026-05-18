package org.example.backend.service;
import org.example.backend.dto.request.AddProjectRequest;
import org.example.backend.dto.response.ProjectResponse;

import java.util.List;

public interface IProjectService {
    ProjectResponse createProject(AddProjectRequest addProjectRequest);
    List<ProjectResponse> getAllProjects();
    ProjectResponse getProjectById(String id);
}
