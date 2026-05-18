package org.example.backend.repository;

import org.example.backend.entity.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITaskRepository extends MongoRepository<Task, String> {
    List<Task> findByProjectId(String projectId);
    List<Task> findBySprintId(String sprintId);
    
    long countByProjectId(String projectId);
    long countByProjectIdAndStatusId(String projectId, String statusId);
}
