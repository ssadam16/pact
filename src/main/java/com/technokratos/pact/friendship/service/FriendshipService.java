package com.technokratos.pact.friendship.service;

import com.technokratos.pact.friendship.dto.FriendshipIncomeRequest;
import com.technokratos.pact.friendship.dto.FriendshipRequest;
import com.technokratos.pact.friendship.exception.FriendshipNotFoundException;
import com.technokratos.pact.friendship.mapper.FriendshipMapper;
import com.technokratos.pact.friendship.model.Friendship;
import com.technokratos.pact.friendship.repository.FriendshipRepository;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import com.technokratos.pact.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final FriendshipMapper friendshipMapper;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public void sendFriendshipRequest(UUID whoId, UUID toWhomId) {
        User who = userRepository.findById(whoId)
                .orElseThrow(() -> UserNotFoundException.byId(whoId));
        User toWhom = userRepository.findById(toWhomId)
                .orElseThrow(() -> UserNotFoundException.byId(toWhomId));

        Friendship friendship = friendshipRepository.save(
                new Friendship(
                        who,
                        toWhom,
                        Friendship.FriendshipStatus.PENDING
                )
        );

        log.info("Friendship request (ID={}) is sent", friendship.getId());
    }

    @Transactional
    public void acceptFriendRequest(UUID requestId) {
        setFriendshipStatus(Friendship.FriendshipStatus.ACCEPTED, requestId);
    }

    @Transactional
    public void declineFriendRequest(UUID requestId) {
        setFriendshipStatus(Friendship.FriendshipStatus.REJECTED, requestId);
    }

    private void setFriendshipStatus(Friendship.FriendshipStatus status, UUID friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> FriendshipNotFoundException.byId(friendshipId));

        if(friendship.getStatus() != status) {
            friendship.setStatus(status);
            friendshipRepository.save(friendship);
            
            log.info("Friendship status was changed (ID={}, newStatus={})", friendshipId, status);
        } else {
            log.debug("Friendship status is already '{}'", status);
        }
    }

    public Set<UserShortProfileResponse> getFriends(UUID userId) {
        log.info("Returning friends set for user (ID={})", userId);
        return friendshipRepository.findFriendIdsByUserIdAndStatus(userId, Friendship.FriendshipStatus.ACCEPTED).stream()
                .map(userService::getShortProfile)
                .collect(Collectors.toSet());
    }

    public Set<FriendshipIncomeRequest> getFriendshipIncomeRequests(UUID toWhom) {
        log.info("Returning friendship requests set for user (ID={})", toWhom);
        return friendshipRepository.findFriendshipByToWhomAndStatus(toWhom, Friendship.FriendshipStatus.PENDING).stream()
                .map(friendshipMapper::toFriendshipIncomeRequest)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void removeFriend(UUID userId1, UUID userId2) {
        Friendship friendship = friendshipRepository.findFriendshipBetweenUsers(userId1, userId2)
                .orElseThrow(() -> FriendshipNotFoundException.between(userId1, userId2));

        friendshipRepository.delete(friendship);

        log.info("Friendship between user1 (ID={}) and user2 (ID={}) is deleted", userId1, userId2);
    }

    public UserShortProfileResponse search(UUID currentUserId, String username) {
        userRepository.findById(currentUserId)
                .orElseThrow(() -> UserNotFoundException.byId(currentUserId));

        User potential = userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));

        Optional<Friendship> friendshipOptional = friendshipRepository.findFriendshipBetweenUsers(currentUserId, potential.getId());

        if (currentUserId.equals(potential.getId())) {
            return null;
        }

        if (friendshipOptional.isPresent()) {
            Friendship friendship = friendshipOptional.get();

            if (friendship.getStatus() == Friendship.FriendshipStatus.ACCEPTED) {
                return null;
            } else if (friendship.getStatus() == Friendship.FriendshipStatus.PENDING) {
                return null;
            } else if (friendship.getStatus() == Friendship.FriendshipStatus.REJECTED) {
                friendshipRepository.delete(friendship);
            }
        }
        return userService.getShortProfile(username);
    }
}
