package com.technokratos.pact.user.controller;

import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRestController {

    @GetMapping("{username}")
    public ResponseEntity<UserShortProfileResponse> getUserShortProfileResponse(@PathVariable String username) {
        return null;
    }
}
