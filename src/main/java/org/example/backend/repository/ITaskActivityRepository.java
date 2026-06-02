package org.example.backend.repository;

import org.example.backend.entity.TaskActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ITaskActivityRepository extends MongoRepository<TaskActivity, String> {
    List<TaskActivity> findByProjectIdAndSprintId(String projectId, String sprintId);
    List<TaskActivity> findByTaskId(String taskId);
    List<TaskActivity> findByProjectId(String projectId);
    List<TaskActivity> findByProjectIdAndCreatedAtBetween(String projectId, LocalDateTime from, LocalDateTime to);
    List<TaskActivity> findBySprintId(String sprintId);
    List<TaskActivity> findByUserIdIn(List<String> userIds);
    List<TaskActivity> findByUserIdInAndProjectIdIn(List<String> userIds, List<String> projectIds);

}
