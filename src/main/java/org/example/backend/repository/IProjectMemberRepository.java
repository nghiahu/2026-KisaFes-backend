package org.example.backend.repository;

import org.example.backend.entity.ProjectMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IProjectMemberRepository extends MongoRepository<ProjectMember, String> {
    List<ProjectMember> findByUserId (String userId);
}
