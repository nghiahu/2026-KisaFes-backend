package org.example.backend.repository;

import org.example.backend.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICategoryRepository extends MongoRepository<Category, String> {
    Optional<Category> findByName(String name);
}
