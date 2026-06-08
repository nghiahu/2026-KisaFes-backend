package org.example.backend.repository;

import org.example.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByUserNameContainingIgnoreCaseOrUserEmailContainingIgnoreCaseOrDetailsContainingIgnoreCase(
            String userName, String userEmail, String details, Pageable pageable);

    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);

    Page<AuditLog> findAll(Pageable pageable);
}
