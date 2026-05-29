package org.example.backend.service;

import org.example.backend.dto.request.EpicRequest;
import org.example.backend.dto.response.EpicResponse;

import java.util.List;

public interface IEpicService {
    EpicResponse createEpic(String projectId, EpicRequest request);
    List<EpicResponse> getEpicsByProject(String projectId);
    EpicResponse updateEpic(String projectId, String epicId, EpicRequest request);
    void deleteEpic(String projectId, String epicId);
}
