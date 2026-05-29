package org.example.backend.repository;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.request.TaskSearchRequest;
import org.example.backend.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ITaskRepositoryCustomImpl implements ITaskRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Task> searchTasks(TaskSearchRequest request) {
        Query query = new Query();

        if (request.getProjectId() != null && !request.getProjectId().trim().isEmpty()) {
            query.addCriteria(Criteria.where("projectId").is(request.getProjectId()));
        }

        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(request.getKeyword(), "i"),
                    Criteria.where("taskKey").regex(request.getKeyword(), "i")
            );
            query.addCriteria(keywordCriteria);
        }

        if (request.getType() != null && !request.getType().trim().isEmpty()) {
            query.addCriteria(Criteria.where("type").is(request.getType()));
        }

        if (request.getStatusId() != null && !request.getStatusId().trim().isEmpty()) {
            query.addCriteria(Criteria.where("statusId").is(request.getStatusId()));
        }

        if (request.getAssigneeId() != null && !request.getAssigneeId().trim().isEmpty()) {
            if ("unassigned".equalsIgnoreCase(request.getAssigneeId())) {
                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("assigneeId").isNull(),
                        Criteria.where("assigneeId").is("")
                ));
            } else {
                query.addCriteria(Criteria.where("assigneeId").is(request.getAssigneeId()));
            }
        }

        if (request.getPriority() != null && !request.getPriority().trim().isEmpty()) {
            query.addCriteria(Criteria.where("priority").is(request.getPriority()));
        }

        long total = mongoTemplate.count(query, Task.class);

        // Frontend sends 1-indexed page
        int pageIndex = Math.max(0, request.getPage() - 1);
        Pageable pageable = PageRequest.of(pageIndex, request.getSize());
        query.with(pageable);

        List<Task> tasks = mongoTemplate.find(query, Task.class);

        return new PageImpl<>(tasks, pageable, total);
    }

    @Override
    public Page<Task> findMyTasks(org.example.backend.dto.request.TaskFilter filter, Pageable pageable) {
        Query query = new Query();

        if (filter.getAssigneeId() != null && !filter.getAssigneeId().trim().isEmpty()) {
            query.addCriteria(Criteria.where("assigneeId").is(filter.getAssigneeId()));
        }

        if (filter.getProjectId() != null && !filter.getProjectId().trim().isEmpty()) {
            query.addCriteria(Criteria.where("projectId").is(filter.getProjectId()));
        }

        if (filter.getStatusId() != null && !filter.getStatusId().trim().isEmpty()) {
            query.addCriteria(Criteria.where("statusId").is(filter.getStatusId()));
        }

        if (filter.getPriority() != null && !filter.getPriority().trim().isEmpty()) {
            query.addCriteria(Criteria.where("priority").is(filter.getPriority()));
        }

        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(filter.getKeyword(), "i"),
                    Criteria.where("taskKey").regex(filter.getKeyword(), "i")
            );
            query.addCriteria(keywordCriteria);
        }

        if (filter.getOverdue() != null && filter.getOverdue()) {
            query.addCriteria(Criteria.where("dueDate").lt(java.time.LocalDateTime.now()));
        } else if (filter.getDueToday() != null && filter.getDueToday()) {
            java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
            java.time.LocalDateTime endOfDay = java.time.LocalDate.now().atTime(23, 59, 59);
            query.addCriteria(Criteria.where("dueDate").gte(startOfDay).lte(endOfDay));
        }

        long total = mongoTemplate.count(query, Task.class);
        query.with(pageable);
        List<Task> tasks = mongoTemplate.find(query, Task.class);

        return new PageImpl<>(tasks, pageable, total);
    }
}
