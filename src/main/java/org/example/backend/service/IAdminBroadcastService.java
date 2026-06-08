package org.example.backend.service;

import org.example.backend.dto.request.AdminBroadcastRequest;
import org.example.backend.dto.response.AdminBroadcastResponse;

import java.util.List;

public interface IAdminBroadcastService {
    List<AdminBroadcastResponse> getAll();
    AdminBroadcastResponse create(AdminBroadcastRequest request, String sentById, String sentByName);
    void delete(String id);
}
