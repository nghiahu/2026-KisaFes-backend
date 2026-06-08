package org.example.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.request.AdminBroadcastRequest;
import org.example.backend.dto.response.AdminBroadcastResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.IAdminBroadcastService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/admin/broadcasts")
public class AdminBroadcastController extends BaseController {

    private final IAdminBroadcastService broadcastService;

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ResponseWrapper<List<AdminBroadcastResponse>>> getAll() {
        return success(broadcastService.getAll(), "Lấy danh sách broadcast thành công");
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ResponseWrapper<AdminBroadcastResponse>> create(
            @Valid @RequestBody AdminBroadcastRequest request,
            Authentication authentication) {
        String senderEmail = authentication.getName();
        return created(broadcastService.create(request, senderEmail, senderEmail), "Gửi broadcast thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<ResponseWrapper<Void>> delete(@PathVariable String id) {
        broadcastService.delete(id);
        return success(null, "Xóa broadcast thành công");
    }
}
