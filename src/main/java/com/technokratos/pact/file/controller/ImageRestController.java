package com.technokratos.pact.file.controller;

import com.technokratos.pact.file.dto.FileInfo;
import com.technokratos.pact.file.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
public class ImageRestController {

    private final MinioService minioService;

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        FileInfo fileInfo = minioService.uploadFile(file, MinioService.Folders.ARTICLES);

        String url = minioService.getFileUrl(fileInfo.getPath());

        return Map.of("url", url);
    }
}