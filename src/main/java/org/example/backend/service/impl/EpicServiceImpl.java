package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.EpicRequest;
import org.example.backend.dto.response.EpicResponse;
import org.example.backend.entity.Epic;
import org.example.backend.entity.Project;
import org.example.backend.repository.IEpicRepository;
import org.example.backend.repository.IProjectRepository;
import org.example.backend.repository.ITaskRepository;
import org.example.backend.security.principle.MyUserDetails;
import org.example.backend.service.IEpicService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EpicServiceImpl implements IEpicService {

    private final IEpicRepository epicRepository;
    private final IProjectRepository projectRepository;
    private final ITaskRepository taskRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private void broadcast(String projectId, String type, Object data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("data", data);
            messagingTemplate.convertAndSend("/topic/project/" + projectId, (Object) payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    @Transactional
    public EpicResponse createEpic(String projectId, EpicRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Dự án không tồn tại"));

        if (project.getMethodology() != Project.Methodology.SCRUM) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Chỉ dự án Scrum mới có Epic");
        }

        MyUserDetails userDetails = (MyUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Epic epic = new Epic();
        epic.setProjectId(projectId);
        epic.setName(request.getName().trim());
        epic.setDescription(request.getDescription());
        epic.setColor(request.getColor() != null ? request.getColor() : "#6366f1");
        epic.setCreatedBy(userDetails.getUserId());

        Epic saved = epicRepository.save(epic);
        EpicResponse response = EpicResponse.fromEntity(saved);
        broadcast(projectId, "EPIC_CREATED", response);
        return response;
    }

    @Override
    public List<EpicResponse> getEpicsByProject(String projectId) {
        return epicRepository.findByProjectId(projectId).stream()
                .map(epic -> {
                    EpicResponse r = EpicResponse.fromEntity(epic);
                    r.setTaskCount((int) taskRepository.findByEpicId(epic.getId()).size());
                    return r;
                })
                .toList();
    }

    @Override
    @Transactional
    public EpicResponse updateEpic(String projectId, String epicId, EpicRequest request) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Epic không tồn tại"));

        if (!epic.getProjectId().equals(projectId)) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Epic không thuộc dự án này");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            epic.setName(request.getName().trim());
        }
        if (request.getDescription() != null) epic.setDescription(request.getDescription());
        if (request.getColor() != null) epic.setColor(request.getColor());

        Epic saved = epicRepository.save(epic);
        EpicResponse response = EpicResponse.fromEntity(saved);
        broadcast(projectId, "EPIC_UPDATED", response);
        return response;
    }

    @Override
    @Transactional
    public void deleteEpic(String projectId, String epicId) {
        Epic epic = epicRepository.findById(epicId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Epic không tồn tại"));

        if (!epic.getProjectId().equals(projectId)) {
            throw new CustomBusinessException(ErrorCode.VALIDATION_ERROR, "Epic không thuộc dự án này");
        }

        // Set epicId = null on all tasks belonging to this epic (non-destructive)
        var tasks = taskRepository.findByEpicId(epicId);
        tasks.forEach(t -> t.setEpicId(null));
        taskRepository.saveAll(tasks);

        epicRepository.delete(epic);
        broadcast(projectId, "EPIC_DELETED", Map.of("epicId", epicId));
    }
}
