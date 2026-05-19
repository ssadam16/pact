package com.technokratos.pact.chat.controller;

import com.technokratos.pact.chat.dto.MediaUploadResponse;
import com.technokratos.pact.chat.service.ChatMediaService;
import com.technokratos.pact.security.model.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/chats/media")
@RequiredArgsConstructor
@Slf4j
public class ChatMediaController {

    private final ChatMediaService chatMediaService;

    @PostMapping("/upload")
    public ResponseEntity<List<MediaUploadResponse>> uploadMedia(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<MediaUploadResponse> responses = chatMediaService.uploadTempMedia(files, currentUser.getId());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/upload-voice")
    public ResponseEntity<MediaUploadResponse> uploadVoice(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<MediaUploadResponse> responses = chatMediaService.uploadTempMedia(
                List.of(file), currentUser.getId(), "voice");
        return ResponseEntity.ok(responses.isEmpty() ? null : responses.get(0));
    }

    @DeleteMapping("/temp")
    public ResponseEntity<Void> cleanupTempMedia(@AuthenticationPrincipal UserDetailsImpl currentUser) {

        chatMediaService.cleanupTempMedia(currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}