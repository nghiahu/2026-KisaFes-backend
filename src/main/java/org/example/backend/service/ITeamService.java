package org.example.backend.service;

import org.example.backend.dto.request.TeamRequest;
import org.example.backend.dto.response.TeamResponse;
import java.util.List;

public interface ITeamService {
    TeamResponse createTeam(TeamRequest request);
    TeamResponse getTeamById(String id);
    TeamResponse updateTeam(String id, TeamRequest request);
    List<TeamResponse> searchTeams(String keyword);
    TeamResponse addMemberToTeam(String teamId, String email, String role);
    void removeMemberFromTeam(String teamId, String userId);
    List<org.example.backend.dto.response.ProjectResponse> getProjectsByTeam(String teamId);
    List<org.example.backend.dto.response.TaskResponse> getTasksByTeam(String teamId);
}
