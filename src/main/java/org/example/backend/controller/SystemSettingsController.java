package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.entity.SystemSettings;
import org.example.backend.repository.ISystemSettingsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class SystemSettingsController extends BaseController {

    private final ISystemSettingsRepository systemSettingsRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_CONFIG', 'ROLE_SUPER ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ResponseWrapper<SystemSettings>> getSettings() {
        SystemSettings settings = systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)
                .orElseGet(() -> {
                    SystemSettings defaultSettings = SystemSettings.builder()
                            .id(SystemSettings.SINGLETON_ID)
                            .companyName("KisaFres")
                            .supportEmail("support@kisafres.com")
                            .allowRegistration(true)
                            .maintenanceMode(false)
                            .maxUploadSizeMB(10)
                            .build();
                    return systemSettingsRepository.save(defaultSettings);
                });

        return success(settings, "Lấy thông tin cài đặt thành công");
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_CONFIG', 'ROLE_SUPER ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ResponseWrapper<SystemSettings>> updateSettings(@RequestBody SystemSettings newSettings) {
        SystemSettings settings = systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)
                .orElse(SystemSettings.builder().id(SystemSettings.SINGLETON_ID).build());

        if (newSettings.getCompanyName() != null) settings.setCompanyName(newSettings.getCompanyName());
        if (newSettings.getSupportEmail() != null) settings.setSupportEmail(newSettings.getSupportEmail());
        settings.setAllowRegistration(newSettings.isAllowRegistration());
        settings.setMaintenanceMode(newSettings.isMaintenanceMode());
        if (newSettings.getMaxUploadSizeMB() > 0) settings.setMaxUploadSizeMB(newSettings.getMaxUploadSizeMB());

        return success(systemSettingsRepository.save(settings), "Cập nhật cài đặt thành công");
    }
}
