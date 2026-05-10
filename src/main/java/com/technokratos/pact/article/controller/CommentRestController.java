package com.technokratos.pact.article.controller;

import com.technokratos.pact.article.dto.CommentCreateRequest;
import com.technokratos.pact.article.dto.CommentResponse;
import com.technokratos.pact.article.service.CommentService;
import com.technokratos.pact.common.ApiResponse;
import com.technokratos.pact.security.model.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentRestController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(@Valid @RequestBody CommentCreateRequest comment,
                                                                      BindingResult bindingResult,
                                                                      @AuthenticationPrincipal UserDetailsImpl currentUser) {

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage()));

            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errors));
        }

        CommentResponse response = commentService.create(currentUser.getId(), comment);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
