package org.example.backend.repository;

import org.example.backend.entity.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITaskRepository extends MongoRepository<Task, String>, ITaskRepositoryCustom {
    List<Task> findByProjectId(String projectId);
    List<Task> findBySprintId(String sprintId);
    /** Backlog: tasks not assigned to any sprint */
    List<Task> findByProjectIdAndSprintIdIsNullOrderByBacklogPositionAsc(String projectId);
    List<Task> findBySprintIdOrderByBoardPositionAsc(String sprintId);
    List<Task> findByEpicId(String epicId);

    long countByProjectId(String projectId);
    long countByProjectIdAndStatusId(String projectId, String statusId);
}

