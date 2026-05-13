package com.technokratos.pact.friendship.controller;

import com.technokratos.pact.friendship.dto.FriendshipRequest;
import com.technokratos.pact.friendship.model.Friendship;
import com.technokratos.pact.friendship.service.FriendshipService;
import com.technokratos.pact.security.model.UserDetailsImpl;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("api/friendships")
@RequiredArgsConstructor
public class FriendshipRestController {

    private final FriendshipService friendshipService;

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<Void> acceptFriendRequest(@PathVariable UUID requestId) {
        friendshipService.acceptFriendRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{requestId}/decline")
    public ResponseEntity<Void> declineFriendRequest(@PathVariable UUID requestId) {
        friendshipService.declineFriendRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{friendId}/remove")
    public ResponseEntity<Void> removeFriend(@PathVariable UUID friendId,
                                             @AuthenticationPrincipal UserDetailsImpl currentUser) {

        friendshipService.removeFriend(currentUser.getId(), friendId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/request/{userId}")
    public ResponseEntity<Void> sendFriendshipRequest(@PathVariable UUID userId,
                                                      @AuthenticationPrincipal UserDetailsImpl currentUser) {

        friendshipService.sendFriendshipRequest(currentUser.getId(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/friends")
    @ResponseBody
    public ResponseEntity<Set<UserShortProfileResponse>> getFriends(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        Set<UserShortProfileResponse> friends = friendshipService.getFriends(currentUser.getId());
        return ResponseEntity.ok(friends);
    }
}
