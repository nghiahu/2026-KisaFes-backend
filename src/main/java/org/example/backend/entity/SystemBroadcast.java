package org.example.backend.entity;

import lombok.*;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "system_broadcasts")
public class SystemBroadcast extends BaseEntity {

    private String title;
    private String message;
    private BroadcastType type;
    private TargetAudience targetAudience;
    private String sentById;
    private String sentByName;

    public enum BroadcastType {
        INFO, WARNING, ERROR, SUCCESS
    }

    public enum TargetAudience {
        ALL, ADMINS, USERS
    }
}
