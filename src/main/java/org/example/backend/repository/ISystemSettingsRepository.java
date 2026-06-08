package org.example.backend.repository;

import org.example.backend.entity.SystemSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ISystemSettingsRepository extends MongoRepository<SystemSettings, String> {
}
