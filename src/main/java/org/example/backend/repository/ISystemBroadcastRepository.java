package org.example.backend.repository;

import org.example.backend.entity.SystemBroadcast;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ISystemBroadcastRepository extends MongoRepository<SystemBroadcast, String> {
    List<SystemBroadcast> findAllByOrderByCreatedAtDesc();
}
