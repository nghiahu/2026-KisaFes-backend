package org.example.backend.repository;

import org.example.backend.entity.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITaskRepository extends MongoRepository<Task, String>, ITaskRepositoryCustom {
    List<Task> findByProjectId(String projectId);
    java.util.Optional<Task> findByTaskKeyAndProjectId(String taskKey, String projectId);
    List<Task> findBySprintId(String sprintId);
    List<Task> findByTeamId(String teamId);
    /** Backlog: tasks not assigned to any sprint */
    List<Task> findByProjectIdAndSprintIdIsNullOrderByBacklogPositionAsc(String projectId);
    /** Backlog variant: also catch empty-string sprintId */
    @org.springframework.data.mongodb.repository.Query("{ 'projectId': ?0, $or: [ { 'sprintId': null }, { 'sprintId': '' } ] }")
    List<Task> findBacklogTasksByProjectId(String projectId);

    List<Task> findBySprintIdOrderByBoardPositionAsc(String sprintId);
    List<Task> findByEpicId(String epicId);
    List<Task> findByProjectIdIn(List<String> projectIds);

    long countByProjectId(String projectId);
    long countByProjectIdAndStatusId(String projectId, String statusId);
    
    long countByAssigneeIdAndResolutionIsNull(String assigneeId);
    long countByAssigneeIdAndResolution(String assigneeId, org.example.backend.entity.Resolution resolution);
    long countByProjectIdAndAssigneeIdAndResolutionIsNull(String projectId, String assigneeId);
    long countByProjectIdAndStatusIdIn(String projectId, List<String> statusIds);
    List<Task> findByProjectIdAndDueDateBeforeAndResolutionIsNull(String projectId, java.time.LocalDateTime date);
    Task findFirstByProjectIdOrderByCreatedAtDesc(String projectId);
}

