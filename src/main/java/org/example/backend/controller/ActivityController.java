package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.AddCommentRequest;
import org.example.backend.dto.response.ActivityResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.entity.Activity;
import org.example.backend.entity.ActivityType;
import org.example.backend.entity.User;
import org.example.backend.repository.IActivityRepository;
import org.example.backend.repository.ITaskRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.security.principle.MyUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController extends BaseController {

    private final IActivityRepository activityRepository;
    private final IUserRepository userRepository;
    private final ITaskRepository taskRepository;

    @GetMapping("/task/{taskId}")
    public ResponseEntity<ResponseWrapper<List<ActivityResponse>>> getActivitiesByTaskId(@PathVariable String taskId) {
        // Verify task exists
        if (!taskRepository.existsById(taskId)) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc");
        }

        List<Activity> activities = activityRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        List<User> users = userRepository.findAll();
        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u, (u1, u2) -> u1));

        List<ActivityResponse> responses = activities.stream().map(a -> {
            ActivityResponse res = new ActivityResponse();
            res.setId(a.getId());
            res.setTaskId(a.getTaskId());
            res.setUserId(a.getUserId());
            res.setType(a.getType());
            res.setContent(a.getContent());
            res.setParentId(a.getParentId());
            res.setCreatedAt(a.getCreatedAt());
            res.setUpdatedAt(a.getUpdatedAt());

            User user = userMap.get(a.getUserId());
            if (user != null) {
                res.setUserName(user.getFullName());
                res.setUserAvatar(user.getAvatar());
            } else {
                res.setUserName("Unknown User");
                res.setUserAvatar(null);
            }
            return res;
        }).collect(Collectors.toList());

        return success(responses);
    }

    @PostMapping("/task/{taskId}/comments")
    public ResponseEntity<ResponseWrapper<ActivityResponse>> addComment(
            @PathVariable String taskId,
            @Valid @RequestBody AddCommentRequest request,
            Authentication authentication) {
        
        if (!taskRepository.existsById(taskId)) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc");
        }

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();

        Activity activity = new Activity();
        activity.setTaskId(taskId);
        activity.setUserId(userId);
        activity.setType(ActivityType.COMMENT);
        activity.setContent(request.getContent());

        Activity saved = activityRepository.save(activity);

        User user = userRepository.findById(userId).orElse(null);

        ActivityResponse response = new ActivityResponse();
        response.setId(saved.getId());
        response.setTaskId(saved.getTaskId());
        response.setUserId(saved.getUserId());
        response.setType(saved.getType());
        response.setContent(saved.getContent());
        response.setCreatedAt(saved.getCreatedAt());
        response.setUpdatedAt(saved.getUpdatedAt());
        
        if (user != null) {
            response.setUserName(user.getFullName());
            response.setUserAvatar(user.getAvatar());
        } else {
            response.setUserName("Unknown User");
        }

        return created(response, "Thêm bình luận thành công");
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<ResponseWrapper<Void>> deleteComment(
            @PathVariable String activityId,
            Authentication authentication) {
        
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy hoạt động"));

        if (activity.getType() != ActivityType.COMMENT) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ có thể xóa bình luận");
        }

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();

        if (!activity.getUserId().equals(userId)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền xóa bình luận này");
        }

        activityRepository.delete(activity);
        return success(null, "Xóa bình luận thành công");
    }
}
