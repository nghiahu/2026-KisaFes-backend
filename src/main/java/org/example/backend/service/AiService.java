package org.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.ai.ProjectIntelligenceDto;
import org.example.backend.dto.request.AddTaskRequest;
import org.example.backend.dto.request.AiConfirmTasksRequest;
import org.example.backend.dto.response.AiGeneratedTaskDto;
import org.example.backend.dto.response.AiTaskGenerationResult;
import org.example.backend.entity.*;
import org.example.backend.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.security.principle.MyUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AiService {

    @Value("${groq.api-key}")
    private String apiKey;

    private final IProjectRepository projectRepository;
    private final IProjectMemberRepository projectMemberRepository;
    private final IUserRepository userRepository;
    private final ITaskRepository taskRepository;
    private final IEpicRepository epicRepository;
    private final ITaskService taskService;
    private final IProjectService projectService;
    private final ProjectIntelligenceService projectIntelligenceService;
    private final ISprintRepository sprintRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private String chatPromptTemplate;
    private String taskPromptTemplate;
    private String editPromptTemplate;
    private String planSprintPromptTemplate;

    @PostConstruct
    public void init() {
        try {
            chatPromptTemplate = new String(new org.springframework.core.io.ClassPathResource("prompts/chat-prompt.txt").getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            taskPromptTemplate = new String(new org.springframework.core.io.ClassPathResource("prompts/task-prompt.txt").getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            editPromptTemplate = new String(new org.springframework.core.io.ClassPathResource("prompts/edit-prompt.txt").getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            planSprintPromptTemplate = new String(new org.springframework.core.io.ClassPathResource("prompts/plan-sprint-prompt.txt").getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load AI prompts", e);
            chatPromptTemplate = "Bạn là KisaFres AI. Dữ liệu dự án:\n%s\nHãy trả lời câu hỏi.";
            taskPromptTemplate = "Dữ liệu dự án:\n%s\nTrả về JSON.";
            editPromptTemplate = "Dữ liệu dự án:\n%s\nTrả về JSON.";
            planSprintPromptTemplate = "Dữ liệu dự án:\n%s\nTrả về JSON.";
        }
    }

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    // ──────────────────────────────────────────────────────────────────────────
    // 1. CHAT (Phase 1 – giữ nguyên)
    // ──────────────────────────────────────────────────────────────────────────
    public String chatWithContext(String projectId, String userMessage) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(projectId, userDetails.getUserId(), Permission.PROJECT_VIEW)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền xem thông tin dự án này");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        // Phase 4A: Lấy thông tin thông minh từ ProjectIntelligenceService
        ProjectIntelligenceDto intelligence = projectIntelligenceService.analyze(projectId);
        String contextJson;
        try {
            List<Task> currentTasks = taskRepository.findByProjectId(projectId);
            List<java.util.Map<String, Object>> taskContext = currentTasks.stream()
                .filter(t -> t.getResolution() == null)
                .limit(100)
                .map(t -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("taskKey", t.getTaskKey());
                    map.put("title", t.getTitle());
                    map.put("statusId", t.getStatusId());
                    map.put("assigneeId", t.getAssigneeId());
                    map.put("reporterId", t.getReporterId());
                    map.put("priority", t.getPriority());
                    map.put("type", t.getType());
                    map.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : null);
                    return map;
                }).collect(java.util.stream.Collectors.toList());

            java.util.Map<String, Object> finalContext = new java.util.HashMap<>();
            finalContext.put("intelligence", intelligence);
            finalContext.put("tasks", taskContext);
            
            contextJson = objectMapper.writeValueAsString(finalContext);
        } catch (Exception e) {
            contextJson = "{}";
        }

        String systemPrompt = chatPromptTemplate.replace("%s", contextJson);

        return callGroq(systemPrompt, userMessage);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. TASK GENERATION (Phase 2)
    // ──────────────────────────────────────────────────────────────────────────
    public AiTaskGenerationResult generateTasksFromDescription(String projectId, String description) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(projectId, userDetails.getUserId(), Permission.TASK_CREATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền tạo công việc trong dự án này");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy dự án"));

        // Sử dụng cấu trúc DTO mới cho AI biết tổng quan dự án
        ProjectIntelligenceDto intelligence = projectIntelligenceService.analyze(projectId);
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(intelligence);
        } catch (Exception e) {
            contextJson = "{}";
        }

        String systemPrompt = taskPromptTemplate.replace("%s", contextJson);

        String rawJson = callGroq(systemPrompt, description);

        // Trích xuất JSON thuần từ response (phòng trường hợp AI thêm markdown code block)
        String cleanJson = extractJson(rawJson);

        try {
            return objectMapper.readValue(cleanJson, AiTaskGenerationResult.class);
        } catch (Exception e) {
            throw new RuntimeException("AI trả về dữ liệu không hợp lệ. Vui lòng thử lại: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. CONFIRM & CREATE TASKS (Phase 2)
    // ──────────────────────────────────────────────────────────────────────────
    public List<String> confirmAndCreateTasks(String projectId, AiConfirmTasksRequest request) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(projectId, userDetails.getUserId(), Permission.TASK_CREATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền tạo công việc trong dự án này");
        }

        // Tạo Epic nếu có tên epic, dùng final để dùng được trong lambda
        final String epicId;
        if (request.getEpicName() != null && !request.getEpicName().isBlank()) {
            Epic epic = new Epic();
            epic.setProjectId(projectId);
            epic.setName(request.getEpicName());
            epic.setDescription(request.getEpicDescription());
            epic.setColor("#6366f1");
            Epic savedEpic = epicRepository.save(epic);
            epicId = savedEpic.getId();
        } else {
            epicId = null;
        }

        final String finalSprintId;
        if (request.getNewSprintName() != null && !request.getNewSprintName().isBlank()) {
            Sprint sprint = new Sprint();
            sprint.setProjectId(projectId);
            sprint.setName(request.getNewSprintName());
            sprint.setStatus("PLANNING"); // Trạng thái mặc định
            Sprint savedSprint = sprintRepository.save(sprint);
            finalSprintId = savedSprint.getId();
        } else if (request.getTargetSprintId() != null && !request.getTargetSprintId().isBlank()) {
            finalSprintId = request.getTargetSprintId();
        } else {
            finalSprintId = null;
        }

        List<String> createdTaskIds = new ArrayList<>();
        for (AiGeneratedTaskDto dto : request.getTasks()) {
            AddTaskRequest taskRequest = new AddTaskRequest();
            taskRequest.setProjectId(projectId);
            taskRequest.setTitle(dto.getTitle());
            taskRequest.setDescription(dto.getDescription());
            taskRequest.setType(dto.getType() != null ? dto.getType() : "task");
            taskRequest.setPriority(dto.getPriority() != null ? dto.getPriority() : "Medium");
            taskRequest.setStoryPoints(dto.getStoryPoints() != null ? dto.getStoryPoints() : 0);
            taskRequest.setAssigneeId(dto.getSuggestedAssigneeId());
            
            if (dto.getDueDate() != null && !dto.getDueDate().isBlank()) {
                try {
                    java.time.LocalDate date = java.time.LocalDate.parse(dto.getDueDate());
                    taskRequest.setDueDate(date.atTime(23, 59, 59)); // Cuối ngày
                } catch (Exception e) {
                    // Ignore invalid date format
                }
            }

            var created = taskService.createTask(taskRequest);

            // Nếu có epicId hoặc sprintId, gán task thông qua repository trực tiếp
            if (epicId != null || finalSprintId != null) {
                taskRepository.findById(created.getId()).ifPresent(task -> {
                    if (epicId != null) task.setEpicId(epicId); 
                    if (finalSprintId != null) task.setSprintId(finalSprintId);
                    taskRepository.save(task);
                });
            }

            createdTaskIds.add(created.getId());
        }

        return createdTaskIds;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. TASK EDITING (Phase 2 - Update)
    // ──────────────────────────────────────────────────────────────────────────
    public org.example.backend.dto.response.AiTaskEditResult generateEditsFromDescription(String projectId, String description) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(projectId, userDetails.getUserId(), Permission.TASK_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền sửa công việc trong dự án này");
        }

        ProjectIntelligenceDto intelligence = projectIntelligenceService.analyze(projectId);
        List<Task> currentTasks = taskRepository.findByProjectId(projectId);
        List<java.util.Map<String, Object>> taskContext = currentTasks.stream()
            .filter(t -> t.getResolution() == null) // Chỉ lấy các task chưa close/resolve
            .limit(100) // Giới hạn 100 task để tránh vượt quá token limit của LLM
            .map(t -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("taskKey", t.getTaskKey());
                map.put("title", t.getTitle());
                map.put("statusId", t.getStatusId());
                map.put("assigneeId", t.getAssigneeId());
                map.put("reporterId", t.getReporterId());
                map.put("priority", t.getPriority());
                map.put("type", t.getType());
                map.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : null);
                map.put("epicId", t.getEpicId());
                map.put("sprintId", t.getSprintId());
                map.put("teamId", t.getTeamId());
                map.put("storyPoints", t.getStoryPoints());
                return map;
            }).collect(java.util.stream.Collectors.toList());

        java.util.Map<String, Object> finalContext = new java.util.HashMap<>();
        finalContext.put("intelligence", intelligence);
        finalContext.put("tasks", taskContext);
        finalContext.put("currentDate", java.time.LocalDate.now().toString());

        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(finalContext);
        } catch (Exception e) {
            contextJson = "{}";
        }

        String systemPrompt = editPromptTemplate.replace("%s", contextJson);
        String rawJson = callGroq(systemPrompt, description);
        String cleanJson = extractJson(rawJson);

        try {
            return objectMapper.readValue(cleanJson, org.example.backend.dto.response.AiTaskEditResult.class);
        } catch (Exception e) {
            throw new RuntimeException("AI trả về dữ liệu không hợp lệ. Vui lòng thử lại: " + e.getMessage());
        }
    }

    public List<String> confirmAndApplyEdits(String projectId, org.example.backend.dto.request.AiConfirmEditsRequest request) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(projectId, userDetails.getUserId(), Permission.TASK_UPDATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền sửa công việc trong dự án này");
        }

        List<String> updatedTaskIds = new ArrayList<>();
        for (var edit : request.getConfirmedEdits()) {
            Task task = null;
            if (edit.getTaskId() != null && !edit.getTaskId().isBlank()) {
                task = taskRepository.findById(edit.getTaskId()).orElse(null);
            }
            if (task == null && edit.getTaskKey() != null && !edit.getTaskKey().isBlank()) {
                task = taskRepository.findByTaskKeyAndProjectId(edit.getTaskKey(), projectId).orElse(null);
            }
            if (task == null) {
                throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy task " + (edit.getTaskKey() != null ? edit.getTaskKey() : ""));
            }
            
            if (!task.getProjectId().equals(projectId)) {
                throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Task không thuộc dự án này");
            }

            switch (edit.getFieldToChange()) {
                case "statusId":
                    taskService.updateTaskStatus(task.getId(), edit.getNewValue());
                    break;
                case "assigneeId":
                    task.setAssigneeId(edit.getNewValue());
                    taskRepository.save(task);
                    break;
                case "dueDate":
                    if (edit.getNewValue() != null) {
                        try {
                            java.time.LocalDateTime date = java.time.LocalDateTime.parse(edit.getNewValue());
                            task.setDueDate(date);
                            taskRepository.save(task);
                        } catch (Exception e) {
                            try {
                                java.time.LocalDate date = java.time.LocalDate.parse(edit.getNewValue().substring(0, 10));
                                task.setDueDate(date.atTime(23, 59, 59));
                                taskRepository.save(task);
                            } catch (Exception ex) {}
                        }
                    } else {
                        task.setDueDate(null);
                        taskRepository.save(task);
                    }
                    break;
                case "priority":
                    task.setPriority(edit.getNewValue());
                    taskRepository.save(task);
                    break;
                case "type":
                    try {
                        task.setType(org.example.backend.entity.TaskType.valueOf(edit.getNewValue().toUpperCase()));
                        taskRepository.save(task);
                    } catch (IllegalArgumentException ignored) {}
                    break;
                case "title":
                    task.setTitle(edit.getNewValue());
                    taskRepository.save(task);
                    break;
            }
            updatedTaskIds.add(task.getId());
        }

        return updatedTaskIds;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. SPRINT PLANNING (AI Auto-plan Sprint)
    // ──────────────────────────────────────────────────────────────────────────
    public org.example.backend.dto.response.AiSprintPlanResult generateSprintPlan(String projectId, String description) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(projectId, userDetails.getUserId(), Permission.TASK_CREATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền lên kế hoạch Sprint trong dự án này");
        }

        ProjectIntelligenceDto intelligence = projectIntelligenceService.analyze(projectId);
        String contextJson;
        try {
            // Chỉ lấy task từ Backlog (sprintId null HOẶC rỗng)
            List<Task> backlogTasks = taskRepository.findBacklogTasksByProjectId(projectId);
            List<java.util.Map<String, Object>> taskContext = backlogTasks.stream()
                .filter(t -> t.getResolution() == null
                          || t.getResolution() == org.example.backend.entity.Resolution.UNRESOLVED)
                .limit(150)
                .map(t -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("taskKey", t.getTaskKey());
                    map.put("id", t.getId());
                    map.put("title", t.getTitle());
                    map.put("priority", t.getPriority());
                    map.put("type", t.getType());
                    map.put("storyPoints", t.getStoryPoints());
                    return map;
                }).collect(java.util.stream.Collectors.toList());

            java.util.Map<String, Object> finalContext = new java.util.HashMap<>();
            finalContext.put("intelligence", intelligence);
            finalContext.put("tasks", taskContext);
            
            contextJson = objectMapper.writeValueAsString(finalContext);
        } catch (Exception e) {
            contextJson = "{}";
        }

        String systemPrompt = planSprintPromptTemplate.replace("%s", contextJson);
        String rawJson = callGroq(systemPrompt, description);
        String cleanJson = extractJson(rawJson);

        try {
            org.example.backend.dto.response.AiRawSprintPlanResult rawResult = objectMapper.readValue(cleanJson, org.example.backend.dto.response.AiRawSprintPlanResult.class);
            
            List<org.example.backend.dto.response.AiPlanTaskDto> selectedTasks = new ArrayList<>();
            if (rawResult.getSelectedTaskIds() != null) {
                for (String rawKey : rawResult.getSelectedTaskIds()) {
                    if (rawKey == null || rawKey.trim().isEmpty()) continue;
                    String keyOrId = rawKey.trim();
                    Task t = taskRepository.findByTaskKeyAndProjectId(keyOrId, projectId).orElse(null);
                    if (t == null) {
                        t = taskRepository.findByTaskKeyAndProjectId(keyOrId.toUpperCase(), projectId).orElse(null);
                    }
                    if (t == null) {
                        t = taskRepository.findById(keyOrId).orElse(null);
                    }
                    if (t != null && t.getProjectId().equals(projectId)) {
                        org.example.backend.dto.response.AiPlanTaskDto dto = new org.example.backend.dto.response.AiPlanTaskDto();
                        dto.setId(t.getId());
                        dto.setTaskKey(t.getTaskKey());
                        dto.setTitle(t.getTitle());
                        dto.setType(t.getType() != null ? t.getType().name() : "TASK");
                        dto.setPriority(t.getPriority());
                        selectedTasks.add(dto);
                    }
                }
            }
            org.example.backend.dto.response.AiSprintPlanResult finalResult = new org.example.backend.dto.response.AiSprintPlanResult();
            finalResult.setSprintName(rawResult.getSprintName());
            finalResult.setSprintGoal(rawResult.getSprintGoal());
            finalResult.setReasoning(rawResult.getReasoning());
            finalResult.setTargetSprintId(rawResult.getTargetSprintId());
            finalResult.setSelectedTasks(selectedTasks);

            return finalResult;

        } catch (Exception e) {
            throw new RuntimeException("AI trả về dữ liệu không hợp lệ. Vui lòng thử lại: " + e.getMessage());
        }
    }

    public List<String> confirmSprintPlan(String projectId, org.example.backend.dto.request.AiConfirmSprintPlanRequest request) {
        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!projectService.hasPermission(projectId, userDetails.getUserId(), Permission.TASK_CREATE)) {
            throw new CustomBusinessException(ErrorCode.PERMISSION_DENIED, "Bạn không có quyền lên kế hoạch Sprint trong dự án này");
        }

        String sprintId;
        if (request.getTargetSprintId() != null && !request.getTargetSprintId().trim().isEmpty()) {
            String rawTargetId = request.getTargetSprintId().trim();
            // Xác minh sprint này tồn tại và thuộc dự án (tránh AI trả về tên sprint thay vì ID)
            Sprint targetSprint = sprintRepository.findById(rawTargetId)
                .filter(s -> s.getProjectId().equals(projectId))
                .orElse(null);
            if (targetSprint == null) {
                // AI trả về tên/giá trị sai — tỏ ra lỗi rõ ràng
                throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Sprint đích không tồn tại (targetSprintId='" + rawTargetId + "'). Vui lòng thử lại.");
            }
            sprintId = targetSprint.getId();
        } else {
            Sprint sprint = new Sprint();
            sprint.setProjectId(projectId);
            sprint.setName(request.getSprintName());
            sprint.setGoal(request.getSprintGoal());
            sprint.setStatus("PLANNING");
            Sprint savedSprint = sprintRepository.save(sprint);
            sprintId = savedSprint.getId();
        }

        List<String> updatedTaskIds = new ArrayList<>();
        final String finalSprintId = sprintId;
        for (String taskId : request.getTaskIds()) {
            taskRepository.findById(taskId).ifPresent(task -> {
                // Chuyển task vào sprint đích (kể cả task đang ở sprint khác do lần thử trước)
                if (task.getProjectId().equals(projectId)) {
                    task.setSprintId(finalSprintId);
                    taskRepository.save(task);
                    updatedTaskIds.add(task.getId());
                }
            });
        }
        return updatedTaskIds;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────────────────────────────────────
    private String callGroq(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.3-70b-versatile"); // Model Llama 3 cực mạnh và siêu tốc của Groq
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(GROQ_URL, entity, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
                    return (String) messageObj.get("content");
                }
            }
            return "Không có phản hồi từ AI.";
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi kết nối tới Groq API: " + e.getMessage());
        }
    }

    /** Trích xuất JSON block từ markdown response (```json ... ```) nếu có */
    private String extractJson(String text) {
        if (text == null) return "{}";
        String trimmed = text.strip();
        // Nếu AI bọc trong markdown code block
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            int end = trimmed.lastIndexOf("```");
            if (start >= 0 && end > start) {
                return trimmed.substring(start + 1, end).strip();
            }
        }
        // Tìm JSON object thuần
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
