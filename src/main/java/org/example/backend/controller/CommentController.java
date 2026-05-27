package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.CommentRequest;
import org.example.backend.dto.response.CommentResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.entity.Comment;
import org.example.backend.entity.User;
import org.example.backend.repository.ICommentRepository;
import org.example.backend.repository.ITaskRepository;
import org.example.backend.repository.IUserRepository;
import org.example.backend.security.principle.MyUserDetails;
import org.example.backend.service.IProjectService;
import org.example.backend.entity.Permission;
import org.example.backend.entity.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController extends BaseController {

    private final ICommentRepository commentRepository;
    private final IUserRepository userRepository;
    private final ITaskRepository taskRepository;
    private final IProjectService projectService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private void broadcastCommentEvent(String taskId, String type, Object data) {
        try {
            taskRepository.findById(taskId).ifPresent(task -> {
                String projectId = task.getProjectId();
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("type", type);
                payload.put("data", data);
                messagingTemplate.convertAndSend("/topic/project/" + projectId, (Object) payload);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<ResponseWrapper<List<CommentResponse>>> getCommentsByTaskId(@PathVariable String taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc");
        }

        List<Comment> comments = commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        List<User> users = userRepository.findAll();
        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u, (u1, u2) -> u1));

        List<CommentResponse> responses = comments.stream().map(c -> {
            CommentResponse res = new CommentResponse();
            res.setId(c.getId());
            res.setTaskId(c.getTaskId());
            res.setUserId(c.getUserId());
            res.setContent(c.getContent());
            res.setParentId(c.getParentId());
            res.setImageUrls(c.getImageUrls());
            res.setReactions(c.getReactions() != null ? c.getReactions() : new HashMap<>());
            res.setCreatedAt(c.getCreatedAt());
            res.setUpdatedAt(c.getUpdatedAt());

            User user = userMap.get(c.getUserId());
            if (user != null) {
                res.setUserName(user.getFullName());
                res.setUserAvatar(user.getAvatar());
            } else {
                res.setUserName("Unknown User");
            }
            return res;
        }).collect(Collectors.toList());

        return success(responses);
    }

    @PostMapping("/task/{taskId}")
    public ResponseEntity<ResponseWrapper<CommentResponse>> createComment(
            @PathVariable String taskId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy công việc"));

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();

        if (!projectService.hasPermission(task.getProjectId(), userId, Permission.COMMENT_CREATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền bình luận trong dự án này");
        }

        Comment comment = new Comment();
        comment.setTaskId(taskId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setImageUrls(request.getImageUrls());
        comment.setReactions(new HashMap<>());

        Comment saved = commentRepository.save(comment);
        User user = userRepository.findById(userId).orElse(null);

        CommentResponse res = new CommentResponse();
        res.setId(saved.getId());
        res.setTaskId(saved.getTaskId());
        res.setUserId(saved.getUserId());
        res.setContent(saved.getContent());
        res.setParentId(saved.getParentId());
        res.setImageUrls(saved.getImageUrls());
        res.setReactions(saved.getReactions());
        res.setCreatedAt(saved.getCreatedAt());
        res.setUpdatedAt(saved.getUpdatedAt());

        if (user != null) {
            res.setUserName(user.getFullName());
            res.setUserAvatar(user.getAvatar());
        } else {
            res.setUserName("Unknown User");
        }

        broadcastCommentEvent(taskId, "CREATE_COMMENT", res);

        return created(res, "Gửi bình luận thành công");
    }

    @PostMapping("/{commentId}/react")
    public ResponseEntity<ResponseWrapper<CommentResponse>> toggleReaction(
            @PathVariable String commentId,
            @RequestParam String emoji,
            Authentication authentication) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận"));

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();

        Map<String, List<String>> reactions = comment.getReactions();
        if (reactions == null) {
            reactions = new HashMap<>();
        }

        // Facebook style: User can only have ONE active emoji reaction per comment.
        // First, check if user has reacted to other emojis and remove them.
        for (String key : new ArrayList<>(reactions.keySet())) {
            if (!key.equals(emoji)) {
                List<String> list = reactions.get(key);
                if (list != null && list.contains(userId)) {
                    list.remove(userId);
                    if (list.isEmpty()) {
                        reactions.remove(key);
                    } else {
                        reactions.put(key, list);
                    }
                }
            }
        }

        List<String> usersList = reactions.get(emoji);
        if (usersList == null) {
            usersList = new ArrayList<>();
        }

        if (usersList.contains(userId)) {
            usersList.remove(userId);
        } else {
            usersList.add(userId);
        }

        if (usersList.isEmpty()) {
            reactions.remove(emoji);
        } else {
            reactions.put(emoji, usersList);
        }

        comment.setReactions(reactions);
        Comment saved = commentRepository.save(comment);

        User author = userRepository.findById(saved.getUserId()).orElse(null);

        CommentResponse res = new CommentResponse();
        res.setId(saved.getId());
        res.setTaskId(saved.getTaskId());
        res.setUserId(saved.getUserId());
        res.setContent(saved.getContent());
        res.setParentId(saved.getParentId());
        res.setImageUrls(saved.getImageUrls());
        res.setReactions(saved.getReactions());
        res.setCreatedAt(saved.getCreatedAt());
        res.setUpdatedAt(saved.getUpdatedAt());

        if (author != null) {
            res.setUserName(author.getFullName());
            res.setUserAvatar(author.getAvatar());
        } else {
            res.setUserName("Unknown User");
        }

        broadcastCommentEvent(saved.getTaskId(), "UPDATE_COMMENT", res);

        return success(res, "Cập nhật cảm xúc thành công");
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ResponseWrapper<Void>> deleteComment(
            @PathVariable String commentId,
            Authentication authentication) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận"));

        MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();

        Task task = taskRepository.findById(comment.getTaskId()).orElse(null);
        boolean hasDeletePermission = task != null && projectService.hasPermission(task.getProjectId(), userId, Permission.COMMENT_DELETE);

        if (!comment.getUserId().equals(userId) && !hasDeletePermission) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền xóa bình luận này");
        }

        commentRepository.delete(comment);

        broadcastCommentEvent(comment.getTaskId(), "DELETE_COMMENT", commentId);

        return success(null, "Xóa bình luận thành công");
    }
}
