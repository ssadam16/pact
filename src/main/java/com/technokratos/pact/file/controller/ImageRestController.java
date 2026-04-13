package com.technokratos.pact.file.controller;

import com.technokratos.pact.file.dto.FileInfo;
import com.technokratos.pact.file.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
@Slf4j
public class ImageRestController {

    private final MinioService minioService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("image") MultipartFile image) {

        try {
            FileInfo fileInfo = minioService.uploadFile(image, MinioService.Folders.ARTICLES);
            String url = minioService.getFileUrl(fileInfo.getPath());

            log.info("Image uploaded successfully: {}", url);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Failed to upload image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}