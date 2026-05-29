package org.example.backend.repository;

import org.example.backend.dto.request.TaskSearchRequest;
import org.example.backend.entity.Task;
import org.springframework.data.domain.Page;

public interface ITaskRepositoryCustom {
    Page<Task> searchTasks(TaskSearchRequest request);
    Page<Task> findMyTasks(org.example.backend.dto.request.TaskFilter filter, org.springframework.data.domain.Pageable pageable);
}
