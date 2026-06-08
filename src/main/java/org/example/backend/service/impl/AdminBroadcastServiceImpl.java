package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.constants.ErrorCode;
import org.example.backend.common.exception.CustomBusinessException;
import org.example.backend.dto.request.AdminBroadcastRequest;
import org.example.backend.dto.response.AdminBroadcastResponse;
import org.example.backend.entity.SystemBroadcast;
import org.example.backend.repository.ISystemBroadcastRepository;
import org.example.backend.service.IAdminBroadcastService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminBroadcastServiceImpl implements IAdminBroadcastService {

    private final ISystemBroadcastRepository broadcastRepository;

    @Override
    public List<AdminBroadcastResponse> getAll() {
        return broadcastRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AdminBroadcastResponse::fromEntity)
                .toList();
    }

    @Override
    public AdminBroadcastResponse create(AdminBroadcastRequest request, String sentById, String sentByName) {
        SystemBroadcast broadcast = SystemBroadcast.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .targetAudience(request.getTargetAudience())
                .sentById(sentById)
                .sentByName(sentByName)
                .build();
        return AdminBroadcastResponse.fromEntity(broadcastRepository.save(broadcast));
    }

    @Override
    public void delete(String id) {
        if (!broadcastRepository.existsById(id)) {
            throw new CustomBusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        broadcastRepository.deleteById(id);
    }
}
