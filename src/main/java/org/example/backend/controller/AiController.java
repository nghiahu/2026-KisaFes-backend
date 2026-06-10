package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.ChatRequest;
import org.example.backend.dto.request.AiConfirmTasksRequest;
import org.example.backend.dto.response.AiTaskGenerationResult;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController extends BaseController {

    private final AiService aiService;

    /** Phase 1 - Chat với ngữ cảnh dự án */
    @PostMapping("/chat")
    public ResponseEntity<ResponseWrapper<String>> chat(
            @RequestParam String projectId,
            @RequestBody ChatRequest request) {
        String response = aiService.chatWithContext(projectId, request.getMessage());
        return success(response);
    }

    /** Phase 2 - AI phân tích mô tả và đề xuất danh sách tasks */
    @PostMapping("/generate-tasks")
    public ResponseEntity<ResponseWrapper<AiTaskGenerationResult>> generateTasks(
            @RequestParam String projectId,
            @RequestBody ChatRequest request) {
        AiTaskGenerationResult result = aiService.generateTasksFromDescription(projectId, request.getMessage());
        return success(result);
    }

    /** Phase 2 - Người dùng xác nhận, tạo thật vào DB */
    @PostMapping("/confirm-tasks")
    public ResponseEntity<ResponseWrapper<List<String>>> confirmTasks(
            @RequestParam String projectId,
            @RequestBody AiConfirmTasksRequest request) {
        List<String> createdIds = aiService.confirmAndCreateTasks(projectId, request);
        return success(createdIds);
    }

    /** Phase 3 - AI đề xuất chỉnh sửa tasks */
    @PostMapping("/edit-tasks")
    public ResponseEntity<ResponseWrapper<org.example.backend.dto.response.AiTaskEditResult>> editTasks(
            @RequestParam String projectId,
            @RequestBody ChatRequest request) {
        org.example.backend.dto.response.AiTaskEditResult result = aiService.generateEditsFromDescription(projectId, request.getMessage());
        return success(result);
    }

    /** Phase 3 - Người dùng xác nhận cập nhật tasks */
    @PostMapping("/confirm-edit-tasks")
    public ResponseEntity<ResponseWrapper<List<String>>> confirmEditTasks(
            @RequestParam String projectId,
            @RequestBody org.example.backend.dto.request.AiConfirmEditsRequest request) {
        List<String> updatedIds = aiService.confirmAndApplyEdits(projectId, request);
        return success(updatedIds);
    }

    /** Phase 4 - AI đề xuất kế hoạch Sprint */
    @PostMapping("/plan-sprint")
    public ResponseEntity<ResponseWrapper<org.example.backend.dto.response.AiSprintPlanResult>> planSprint(
            @RequestParam String projectId,
            @RequestBody ChatRequest request) {
        org.example.backend.dto.response.AiSprintPlanResult result = aiService.generateSprintPlan(projectId, request.getMessage());
        return success(result);
    }

    /** Phase 4 - Xác nhận tạo Sprint từ kế hoạch AI */
    @PostMapping("/confirm-sprint-plan")
    public ResponseEntity<ResponseWrapper<List<String>>> confirmSprintPlan(
            @RequestParam String projectId,
            @RequestBody org.example.backend.dto.request.AiConfirmSprintPlanRequest request) {
        List<String> updatedIds = aiService.confirmSprintPlan(projectId, request);
        return success(updatedIds);
    }
}
