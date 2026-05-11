package org.example.backend.repository;

import org.example.backend.entity.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}
