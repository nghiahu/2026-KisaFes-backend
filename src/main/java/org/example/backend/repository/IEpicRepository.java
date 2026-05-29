package org.example.backend.repository;

import org.example.backend.entity.Epic;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEpicRepository extends MongoRepository<Epic, String> {
    List<Epic> findByProjectId(String projectId);
    void deleteAllByProjectId(String projectId);
}
