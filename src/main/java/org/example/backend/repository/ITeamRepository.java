package org.example.backend.repository;

import org.example.backend.entity.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ITeamRepository extends MongoRepository<Team, String> {
    List<Team> findByNameContainingIgnoreCase(String name);
}
