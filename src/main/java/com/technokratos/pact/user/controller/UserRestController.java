package com.technokratos.pact.user.controller;

import com.technokratos.pact.user.dto.UserShortProfileResponse;
import com.technokratos.pact.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;

    @GetMapping("{username}")
    public ResponseEntity<UserShortProfileResponse> getUserShortProfileResponse(@PathVariable String username) {
        UserShortProfileResponse profile = userService.getShortProfile(username);
        return ResponseEntity.ok(profile);
    }
}
