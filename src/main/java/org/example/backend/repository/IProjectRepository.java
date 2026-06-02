package org.example.backend.repository;

import org.example.backend.entity.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IProjectRepository extends MongoRepository<Project, String> {
    Optional<Project> findByCode(String code);

    @org.springframework.data.mongodb.repository.Query("{ 'teams.teamId': { $in: ?0 } }")
    java.util.List<Project> findProjectsByTeamIds(java.util.List<String> teamIds);
}
