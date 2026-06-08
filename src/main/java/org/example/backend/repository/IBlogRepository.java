package org.example.backend.repository;

import org.example.backend.entity.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IBlogRepository extends MongoRepository<Blog, String> {

    @Query("{ '$and': [ " +
           "{ '$or': [ { 'title': { '$regex': ?0, '$options': 'i' } }, { 'tags': { '$regex': ?0, '$options': 'i' } } ] }, " +
           "{ $cond: [ { '$ne': [?1, null] }, { 'status': ?1 }, {} ] } " +
           "] }")
    Page<Blog> findBySearchAndStatus(String search, Blog.BlogStatus status, Pageable pageable);

    @Query("{ '$or': [ { 'title': { '$regex': ?0, '$options': 'i' } }, { 'tags': { '$regex': ?0, '$options': 'i' } } ] }")
    Page<Blog> findBySearch(String search, Pageable pageable);

    Page<Blog> findByStatus(Blog.BlogStatus status, Pageable pageable);

    List<Blog> findByStatusAndPublishAtBefore(Blog.BlogStatus status, LocalDateTime now);

    Page<Blog> findAll(Pageable pageable);
}
