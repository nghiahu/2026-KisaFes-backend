package org.example.backend.entity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.backend.common.base.BaseEntity;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "users")
public class User extends BaseEntity {

    @Indexed(unique = true)
    private String email;
    private String password;
    private String fullName;

    @Indexed(unique = true)
    private String userName;
    private String avatar;
    private String bio;

    private Set<Role> roles;
    private Set<String> projectIds;

    @Builder.Default
    private boolean active = true;
}
