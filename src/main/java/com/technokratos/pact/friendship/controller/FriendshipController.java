package com.technokratos.pact.friendship.controller;

import com.technokratos.pact.friendship.model.Friendship;
import com.technokratos.pact.friendship.service.FriendshipService;
import com.technokratos.pact.security.model.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/friendship")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @GetMapping
    public String getFriendshipPage(Model model, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        model.addAttribute("friends", friendshipService.getFriends(currentUser.getId()));
        model.addAttribute("friendshipRequests", friendshipService.getFriendshipIncomeRequests(currentUser.getId()));
        return "user/friendship";
    }
}
