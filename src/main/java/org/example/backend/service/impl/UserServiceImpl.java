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
        return UserProfileResponse.fromEntity(updatedUser);
    }
}
