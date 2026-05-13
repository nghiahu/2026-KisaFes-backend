package org.example.backend.security.principle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private User user;

    private Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {

        return user.getEmail();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getId() {
        return user.getId();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public String getAvatar() {
        return user.getAvatar();
    }
}
