package org.example.backend.service;

import org.example.backend.dto.request.CompleteSprintRequest;
import org.example.backend.dto.request.SprintRequest;
import org.example.backend.dto.request.UpdateSprintRequest;
import org.example.backend.dto.response.SprintResponse;
import org.example.backend.dto.response.TaskResponse;

import java.util.List;

public interface ISprintService {
    SprintResponse createSprint(String projectId, SprintRequest request);
    List<SprintResponse> getSprintsByProject(String projectId);
    SprintResponse getActiveSprint(String projectId);
    SprintResponse startSprint(String projectId, String sprintId);

    /** Complete sprint and migrate incomplete tasks */
    SprintResponse completeSprintWithMigration(String projectId, String sprintId, CompleteSprintRequest request);

    /** Update sprint metadata (name, goal, dates) */
    SprintResponse updateSprint(String projectId, String sprintId, UpdateSprintRequest request);

    /** Delete a PLANNING sprint only */
    void deleteSprint(String projectId, String sprintId);

    /** Get all tasks for a sprint */
    List<TaskResponse> getSprintTasks(String projectId, String sprintId);

    /** Get backlog tasks (sprintId == null) */
    List<TaskResponse> getBacklog(String projectId);
}
