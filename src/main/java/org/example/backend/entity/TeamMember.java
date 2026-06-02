package org.example.backend.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "team_members")
public class TeamMember extends BaseEntity {
    private String teamId;
    private String userId;
    
    // Role can be 'ADMIN' or 'MEMBER'
    private String role;
    
    private LocalDateTime joinedAt;
}
