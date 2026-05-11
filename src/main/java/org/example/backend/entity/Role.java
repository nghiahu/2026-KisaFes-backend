package org.example.backend.entity;

import lombok.*;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "roles")
public class Role extends BaseEntity {

    @Indexed(unique = true)
    private String name;
    private Set<String> permissions;


    private boolean isSystemRole = false;
}
