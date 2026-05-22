package com.technokratos.pact.friendship.service;

import com.technokratos.pact.friendship.dto.FriendshipIncomeRequest;
import com.technokratos.pact.friendship.exception.FriendshipNotFoundException;
import com.technokratos.pact.friendship.mapper.FriendshipMapper;
import com.technokratos.pact.friendship.model.Friendship;
import com.technokratos.pact.friendship.repository.FriendshipRepository;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import com.technokratos.pact.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendshipMapper friendshipMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FriendshipService friendshipService;

    private UUID whoId;
    private UUID toWhomId;
    private User who;
    private User toWhom;

    @BeforeEach
    void setUp() {
        whoId = UUID.randomUUID();
        toWhomId = UUID.randomUUID();
        who = User.builder().id(whoId).username("said").build();
        toWhom = User.builder().id(toWhomId).username("anna").build();
    }

    @Test
    void sendFriendshipRequest_savesPending() {
        when(userRepository.findById(whoId)).thenReturn(Optional.of(who));
        when(userRepository.findById(toWhomId)).thenReturn(Optional.of(toWhom));
        when(friendshipRepository.save(any(Friendship.class)))
                .thenReturn(new Friendship(who, toWhom, Friendship.FriendshipStatus.PENDING));

        friendshipService.sendFriendshipRequest(whoId, toWhomId);

        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    void sendFriendshipRequest_senderMissing_throws() {
        when(userRepository.findById(whoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.sendFriendshipRequest(whoId, toWhomId))
                .isInstanceOf(UserNotFoundException.class);
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void acceptFriendRequest_setsAccepted() {
        UUID id = UUID.randomUUID();
        Friendship friendship = new Friendship(who, toWhom, Friendship.FriendshipStatus.PENDING);

        when(friendshipRepository.findById(id)).thenReturn(Optional.of(friendship));

        friendshipService.acceptFriendRequest(id);

        assertThat(friendship.getStatus()).isEqualTo(Friendship.FriendshipStatus.ACCEPTED);
        verify(friendshipRepository).save(friendship);
    }

    @Test
    void declineFriendRequest_setsRejected() {
        UUID id = UUID.randomUUID();
        Friendship friendship = new Friendship(who, toWhom, Friendship.FriendshipStatus.PENDING);

        when(friendshipRepository.findById(id)).thenReturn(Optional.of(friendship));

        friendshipService.declineFriendRequest(id);

        assertThat(friendship.getStatus()).isEqualTo(Friendship.FriendshipStatus.REJECTED);
    }

    @Test
    void acceptFriendRequest_missing_throws() {
        UUID id = UUID.randomUUID();
        when(friendshipRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.acceptFriendRequest(id))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    void getFriends_mapsThroughUserService() {
        when(friendshipRepository.findFriendIdsByUserIdAndStatus(whoId, Friendship.FriendshipStatus.ACCEPTED))
                .thenReturn(Set.of(toWhomId));

        UserShortProfileResponse profile = new UserShortProfileResponse();
        profile.setId(toWhomId);

        when(userService.getShortProfile(toWhomId)).thenReturn(profile);

        Set<UserShortProfileResponse> result = friendshipService.getFriends(whoId);

        assertThat(result).containsExactly(profile);
    }

    @Test
    void getFriendshipIncomeRequests_mapsRequests() {
        Friendship f = new Friendship(toWhom, who, Friendship.FriendshipStatus.PENDING);
        when(friendshipRepository.findFriendshipByToWhomAndStatus(whoId, Friendship.FriendshipStatus.PENDING))
                .thenReturn(Set.of(f));

        FriendshipIncomeRequest dto = new FriendshipIncomeRequest(UUID.randomUUID(), null);
        when(friendshipMapper.toFriendshipIncomeRequest(f)).thenReturn(dto);

        Set<FriendshipIncomeRequest> result = friendshipService.getFriendshipIncomeRequests(whoId);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void removeFriend_deletesExisting() {
        Friendship f = new Friendship(who, toWhom, Friendship.FriendshipStatus.ACCEPTED);
        when(friendshipRepository.findFriendshipBetweenUsers(whoId, toWhomId)).thenReturn(Optional.of(f));

        friendshipService.removeFriend(whoId, toWhomId);

        verify(friendshipRepository).delete(f);
    }

    @Test
    void removeFriend_missing_throws() {
        when(friendshipRepository.findFriendshipBetweenUsers(whoId, toWhomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.removeFriend(whoId, toWhomId))
                .isInstanceOf(FriendshipNotFoundException.class);
    }

    @Test
    void search_self_returnsNull() {
        when(userRepository.findById(whoId)).thenReturn(Optional.of(who));
        when(userRepository.findByUsername("said")).thenReturn(Optional.of(who));
        when(friendshipRepository.findFriendshipBetweenUsers(whoId, whoId)).thenReturn(Optional.empty());

        assertThat(friendshipService.search(whoId, "said")).isNull();
    }

    @Test
    void search_alreadyAccepted_returnsNull() {
        Friendship f = new Friendship(who, toWhom, Friendship.FriendshipStatus.ACCEPTED);

        when(userRepository.findById(whoId)).thenReturn(Optional.of(who));
        when(userRepository.findByUsername("anna")).thenReturn(Optional.of(toWhom));
        when(friendshipRepository.findFriendshipBetweenUsers(whoId, toWhomId)).thenReturn(Optional.of(f));

        assertThat(friendshipService.search(whoId, "anna")).isNull();
    }

    @Test
    void search_pending_returnsNull() {
        Friendship f = new Friendship(who, toWhom, Friendship.FriendshipStatus.PENDING);
        when(userRepository.findById(whoId)).thenReturn(Optional.of(who));
        when(userRepository.findByUsername("anna")).thenReturn(Optional.of(toWhom));
        when(friendshipRepository.findFriendshipBetweenUsers(whoId, toWhomId)).thenReturn(Optional.of(f));

        assertThat(friendshipService.search(whoId, "anna")).isNull();
    }

    @Test
    void search_rejected_deletesAndReturnsProfile() {
        Friendship f = new Friendship(who, toWhom, Friendship.FriendshipStatus.REJECTED);

        when(userRepository.findById(whoId)).thenReturn(Optional.of(who));
        when(userRepository.findByUsername("anna")).thenReturn(Optional.of(toWhom));
        when(friendshipRepository.findFriendshipBetweenUsers(whoId, toWhomId)).thenReturn(Optional.of(f));

        UserShortProfileResponse profile = new UserShortProfileResponse();

        when(userService.getShortProfile("anna")).thenReturn(profile);

        UserShortProfileResponse result = friendshipService.search(whoId, "anna");

        verify(friendshipRepository).delete(f);
        assertThat(result).isSameAs(profile);
    }

    @Test
    void search_noFriendship_returnsProfile() {
        when(userRepository.findById(whoId)).thenReturn(Optional.of(who));
        when(userRepository.findByUsername("anna")).thenReturn(Optional.of(toWhom));
        when(friendshipRepository.findFriendshipBetweenUsers(whoId, toWhomId)).thenReturn(Optional.empty());

        UserShortProfileResponse profile = new UserShortProfileResponse();

        when(userService.getShortProfile("anna")).thenReturn(profile);

        assertThat(friendshipService.search(whoId, "anna")).isSameAs(profile);
    }

    @Test
    void search_currentUserMissing_throws() {
        when(userRepository.findById(whoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendshipService.search(whoId, "anna"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
