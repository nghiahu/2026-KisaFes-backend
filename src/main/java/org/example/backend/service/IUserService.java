package org.example.backend.service;

import org.example.backend.common.base.BaseService;
import org.example.backend.entity.User;
import org.example.backend.dto.request.UpdateProfileRequest;
import org.example.backend.dto.response.UserProfileResponse;

public interface IUserService extends BaseService<User, String> {
    UserProfileResponse getUserProfile(String email);
    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);
    java.util.List<org.example.backend.dto.response.UserSearchResponse> searchUsers(String keyword);
}
