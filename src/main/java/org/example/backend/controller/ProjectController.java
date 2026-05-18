package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.AddProjectRequest;
import org.example.backend.dto.response.ProjectResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.IProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

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
}
