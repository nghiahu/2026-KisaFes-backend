package org.example.backend.repository;

import org.example.backend.entity.Sprint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ISprintRepository extends MongoRepository<Sprint, String> {
    List<Sprint> findByProjectId(String projectId);
    Optional<Sprint> findFirstByProjectIdAndStatus(String projectId, String status);
}
