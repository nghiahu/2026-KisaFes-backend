package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.EpicRequest;
import org.example.backend.dto.response.EpicResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.IEpicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/epics")
@RequiredArgsConstructor
public class EpicController extends BaseController {

    private final IEpicService epicService;

    @PostMapping
    public ResponseEntity<ResponseWrapper<EpicResponse>> createEpic(
            @PathVariable String projectId,
            @Valid @RequestBody EpicRequest request) {
        return created(epicService.createEpic(projectId, request), "Tạo epic thành công");
    }

    @GetMapping
    public ResponseEntity<ResponseWrapper<List<EpicResponse>>> getEpics(@PathVariable String projectId) {
        return success(epicService.getEpicsByProject(projectId));
    }

    @PutMapping("/{epicId}")
    public ResponseEntity<ResponseWrapper<EpicResponse>> updateEpic(
            @PathVariable String projectId,
            @PathVariable String epicId,
            @Valid @RequestBody EpicRequest request) {
        return success(epicService.updateEpic(projectId, epicId, request), "Cập nhật epic thành công");
    }

    @DeleteMapping("/{epicId}")
    public ResponseEntity<ResponseWrapper<Void>> deleteEpic(
            @PathVariable String projectId,
            @PathVariable String epicId) {
        epicService.deleteEpic(projectId, epicId);
        return success(null, "Xóa epic thành công");
    }
}
