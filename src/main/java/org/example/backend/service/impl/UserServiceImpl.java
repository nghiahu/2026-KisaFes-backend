package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseServiceImpl;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.UpdateProfileRequest;
import org.example.backend.dto.response.UserProfileResponse;
import org.example.backend.entity.User;
import org.example.backend.repository.IUserRepository;
import org.example.backend.service.IUserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends BaseServiceImpl<User, String> implements IUserService {

    private final IUserRepository userRepository;

    public UserServiceImpl(IUserRepository userRepository) {
        super(userRepository);
        this.userRepository = userRepository;
    }

    @Override
    public UserProfileResponse getUserProfile(String identifier) {
        User user = userRepository.findByUserNameOrEmail(identifier, identifier)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResponse.fromEntity(user);
    }

    @Override
    public UserProfileResponse updateProfile(String identifier, UpdateProfileRequest request) {
        User user = userRepository.findByUserNameOrEmail(identifier, identifier)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.USER_NOT_FOUND));

        user.setFullName(request.getFullName());
        user.setUserName(request.getUserName());
        user.setBio(request.getBio());
        user.setPublic(request.isPublic());
        
        if (request.getAvatar() != null && !request.getAvatar().isBlank()) {
            user.setAvatar(request.getAvatar());
        }

        User updatedUser = userRepository.save(user);
        return mapToProfileResponse(user);
    }

    @Override
    public java.util.List<org.example.backend.dto.response.UserSearchResponse> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String kw = keyword.trim();
        return userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(kw, kw).stream()
                .filter(User::isActive)
                .map(user -> org.example.backend.dto.response.UserSearchResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .avatar(user.getAvatar())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.fromEntity(user);
    }
}
