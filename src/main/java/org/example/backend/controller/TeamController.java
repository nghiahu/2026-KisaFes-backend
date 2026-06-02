package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.TeamRequest;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.dto.response.TeamResponse;
import org.example.backend.repository.IProjectRepository;
import org.example.backend.repository.ITeamMemberRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.service.ITeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController extends BaseController {

    private final ITeamService teamService;
    private final IProjectRepository projectRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final IUserRepository userRepository;

    @PostMapping
    public ResponseEntity<ResponseWrapper<TeamResponse>> createTeam(@RequestBody TeamRequest request) {
        return success(teamService.createTeam(request), "Tạo Team thành công");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseWrapper<TeamResponse>> getTeamById(@PathVariable String id) {
        return success(teamService.getTeamById(id), "Lấy thông tin Team thành công");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseWrapper<TeamResponse>> updateTeam(@PathVariable String id, @RequestBody TeamRequest request) {
        return success(teamService.updateTeam(id, request), "Cập nhật Team thành công");
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseWrapper<List<TeamResponse>>> searchTeams(@RequestParam(required = false) String keyword) {
        return success(teamService.searchTeams(keyword), "Tìm kiếm Team thành công");
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<ResponseWrapper<TeamResponse>> addMember(@PathVariable String teamId,
                                                                 @RequestParam String email,
                                                                 @RequestParam(required = false) String role) {
        return success(teamService.addMemberToTeam(teamId, email, role), "Thêm thành viên thành công");
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<ResponseWrapper<Void>> removeMember(@PathVariable String teamId, @PathVariable String userId) {
        teamService.removeMemberFromTeam(teamId, userId);
        return success(null, "Xóa thành viên thành công");
    }

    @GetMapping("/{teamId}/projects")
    public ResponseEntity<ResponseWrapper<List<org.example.backend.dto.response.ProjectResponse>>> getTeamProjects(@PathVariable String teamId) {
        return success(teamService.getProjectsByTeam(teamId), "Lấy danh sách dự án thành công");
    }

    @GetMapping("/{teamId}/tasks")
    public ResponseEntity<ResponseWrapper<List<org.example.backend.dto.response.TaskResponse>>> getTeamTasks(@PathVariable String teamId) {
        return success(teamService.getTasksByTeam(teamId), "Lấy danh sách công việc thành công");
    }
}
