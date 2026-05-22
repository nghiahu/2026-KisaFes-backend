package org.example.backend.service;

import org.example.backend.dto.request.SprintRequest;
import org.example.backend.dto.response.SprintResponse;

import java.util.List;

public interface ISprintService {
    /** Tạo sprint mới cho dự án (trạng thái PLANNING) */
    SprintResponse createSprint(String projectId, SprintRequest request);

    /** Lấy danh sách tất cả sprint của một dự án */
    List<SprintResponse> getSprintsByProject(String projectId);

    /** Lấy sprint đang active của dự án */
    SprintResponse getActiveSprint(String projectId);

    /** Bắt đầu sprint (PLANNING → ACTIVE). Chỉ một sprint được active tại một thời điểm */
    SprintResponse startSprint(String projectId, String sprintId);

    /** Hoàn thành sprint (ACTIVE → COMPLETED) */
    SprintResponse completeSprint(String projectId, String sprintId);
}
