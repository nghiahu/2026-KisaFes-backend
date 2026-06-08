package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.response.AdminUserDetailResponse;
import org.example.backend.dto.response.AdminUserResponse;
import org.example.backend.dto.response.PageResponse;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.IAdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/admin/users")
public class AdminUserController extends BaseController {

    private final IAdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
    public ResponseEntity<ResponseWrapper<PageResponse<AdminUserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String roleType,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        return success(adminUserService.getAllUsers(page, size, search, roleType, status, sortBy, sortDirection), "Lấy danh sách người dùng thành công");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'USER_MANAGE')")
    public ResponseEntity<ResponseWrapper<AdminUserDetailResponse>> getUserDetail(@PathVariable String id) {
        return success(adminUserService.getUserDetail(id), "Lấy chi tiết người dùng thành công");
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<ResponseWrapper<AdminUserResponse>> changeRole(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return success(adminUserService.changeUserRole(id, body.get("role")), "Thay đổi vai trò thành công");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<ResponseWrapper<AdminUserResponse>> toggleStatus(@PathVariable String id) {
        return success(adminUserService.toggleUserStatus(id), "Cập nhật trạng thái thành công");
    }

    @PostMapping("/bulk-action")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ResponseEntity<ResponseWrapper<Void>> bulkAction(@RequestBody Map<String, Object> body) {
        String action = (String) body.get("action");
        @SuppressWarnings("unchecked")
        List<String> userIds = (List<String>) body.get("userIds");
        adminUserService.bulkAction(action, userIds);
        return success(null, "Thực hiện bulk action thành công");
    }
}
