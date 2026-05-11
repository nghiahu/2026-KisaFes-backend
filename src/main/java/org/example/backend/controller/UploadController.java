package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.base.BaseController;
import org.example.backend.dto.response.ResponseWrapper;
import org.example.backend.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController extends BaseController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/image")
    public ResponseEntity<ResponseWrapper<Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadImage(file);
            return success((Object) url, "Upload ảnh thành công");
        } catch (IOException e) {
            return error(HttpStatus.BAD_REQUEST, "Lỗi upload ảnh: " + e.getMessage());
        }
    }
}
