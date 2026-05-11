package org.example.backend.repository;

import org.example.backend.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserRepository extends MongoRepository<User, String> {
    Optional<User> findByUserNameOrEmail(String userName, String email);
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsUserByUserName(String username);
    Optional<User> findByEmailOrUserName(String email, String username);
}
