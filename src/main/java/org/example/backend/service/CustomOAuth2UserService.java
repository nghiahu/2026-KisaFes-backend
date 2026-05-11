package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.AppConstants;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.entity.Role;
import org.example.backend.entity.User;
import org.example.backend.repository.IRoleRepository;
import org.example.backend.repository.IUserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Xác định login từ Google hay GitHub
        String clientRegistrationId = userRequest.getClientRegistration().getRegistrationId();

        return processOAuth2User(oAuth2User, clientRegistrationId);
    }

    private OAuth2User processOAuth2User(OAuth2User oAuth2User, String provider) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        if (email == null || email.isBlank()) {
            return oAuth2User;
        }

        // GitHub có thể không trả về email công khai trong attributes chính
        if (email == null && "github".equals(provider)) {
            // Logic xử lý lấy email riêng cho GitHub nếu cần
        }

        userRepository.findByEmail(email).orElseGet(() -> {
            Role userRole = roleRepository.findByName(AppConstants.ROLE_USER)
                    .orElseThrow(() -> new CustomBusinessException(ErrorCode.ROLE_NOT_FOUND));
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName((String) attributes.get("name"));
            newUser.setAvatar(provider.equals("google")
                    ? (String) attributes.get("picture")
                    : (String) attributes.get("avatar_url"));
            newUser.setUserName(resolveUsernameFromEmail(email));
            newUser.setRoles(Set.of(userRole));
            newUser.setActive(true);
            return userRepository.save(newUser);
        });

        return oAuth2User;
    }

    private String resolveUsernameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "oauth_user";
        }
        return email.split("@")[0];
    }
}