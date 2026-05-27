package org.example.backend.repository;

import org.example.backend.entity.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IActivityRepository extends MongoRepository<Activity, String> {
    List<Activity> findByTaskIdOrderByCreatedAtDesc(String taskId);
}
