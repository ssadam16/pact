package com.technokratos.pact.chat.service;

import com.technokratos.pact.chat.dto.ChatCreateRequest;
import com.technokratos.pact.chat.dto.ChatResponse;
import com.technokratos.pact.chat.dto.MessageResponse;
import com.technokratos.pact.chat.dto.MessageShortResponse;
import com.technokratos.pact.chat.dto.SendMessageRequest;
import com.technokratos.pact.chat.exception.ChatNotFoundException;
import com.technokratos.pact.chat.exception.MessageNotFoundException;
import com.technokratos.pact.chat.mapper.ChatMapper;
import com.technokratos.pact.chat.mapper.ChatMessageMapper;
import com.technokratos.pact.chat.model.Chat;
import com.technokratos.pact.chat.model.ChatMessage;
import com.technokratos.pact.chat.repository.ChatMessageRepository;
import com.technokratos.pact.chat.repository.ChatRepository;
import com.technokratos.pact.file.service.AvatarService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private ChatMessageMapper messageMapper;

    @Mock
    private ChatMediaService mediaService;

    @Mock
    private UserService userService;

    @Mock
    private AvatarService avatarService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatService chatService;

    private UUID userId;
    private UUID otherId;
    private UUID chatId;
    private User me;
    private User other;
    private Chat chat;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherId = UUID.randomUUID();
        chatId = UUID.randomUUID();
        me = User.builder().id(userId).username("said").build();
        other = User.builder().id(otherId).username("anna").build();
        chat = Chat.builder().id(chatId).firstUser(me).secondUser(other).build();
    }

    private void stubChatResponseDeps() {
        UserShortProfileResponse interlocutor = new UserShortProfileResponse();
        interlocutor.setId(otherId);

        lenient().when(userService.getShortProfile(any(UUID.class))).thenReturn(interlocutor);
        lenient().when(messageRepository.findFirstByChatIdOrderByCreatedAtDesc(chatId)).thenReturn(Optional.empty());
        lenient().when(messageRepository.countUnreadMessages(eq(chatId), any())).thenReturn(0L);
    }

    @Test
    void createChat_existing_returnsExisting() {
        ChatCreateRequest request = new ChatCreateRequest();
        request.setSecondUserId(otherId);

        when(chatRepository.findChatBetweenUsers(userId, otherId)).thenReturn(Optional.of(chat));
        stubChatResponseDeps();

        ChatResponse result = chatService.createChat(request, userId);

        assertThat(result.getId()).isEqualTo(chatId);
        verify(chatRepository, never()).save(any());
    }

    @Test
    void createChat_new_savesChat() {
        ChatCreateRequest request = new ChatCreateRequest();
        request.setSecondUserId(otherId);

        when(chatRepository.findChatBetweenUsers(userId, otherId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(me));
        when(userRepository.findById(otherId)).thenReturn(Optional.of(other));
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        stubChatResponseDeps();

        ChatResponse result = chatService.createChat(request, userId);

        assertThat(result.getId()).isEqualTo(chatId);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void createChat_secondUserMissing_throws() {
        ChatCreateRequest request = new ChatCreateRequest();
        request.setSecondUserId(otherId);

        when(chatRepository.findChatBetweenUsers(userId, otherId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(me));
        when(userRepository.findById(otherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.createChat(request, userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUserChats_mapsPage() {
        Page<Chat> page = new PageImpl<>(List.of(chat));

        when(chatRepository.findAllByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        stubChatResponseDeps();

        Page<ChatResponse> result = chatService.getUserChats(userId, 0);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getChatMessages_accessDenied_throws() {
        UUID stranger = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.getChatMessages(chatId, stranger, 0))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getChatMessages_chatMissing_throws() {
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getChatMessages(chatId, userId, 0))
                .isInstanceOf(ChatNotFoundException.class);
    }

    @Test
    void getChatMessages_mapsMessages() {
        ChatMessage msg = ChatMessage.builder()
                .id(UUID.randomUUID()).author(me).chat(chat)
                .status(ChatMessage.MessageStatus.SENT).build();

        Page<ChatMessage> page = new PageImpl<>(List.of(msg));

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdOrderByCreatedAtDesc(eq(chatId), any(Pageable.class))).thenReturn(page);

        MessageResponse response = new MessageResponse();
        UserShortProfileResponse author = new UserShortProfileResponse();
        author.setId(userId);
        response.setAuthor(author);

        when(messageMapper.toResponse(msg)).thenReturn(response);

        Page<MessageResponse> result = chatService.getChatMessages(chatId, userId, 0);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void sendMessage_emptyContentNoMedia_returnsEmpty() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("");

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(me));

        List<MessageResponse> result = chatService.sendMessage(chatId, request, userId);

        assertThat(result).isEmpty();
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_savesAndBroadcasts() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("hi there");

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findById(userId)).thenReturn(Optional.of(me));

        ChatMessage saved = ChatMessage.builder()
                .id(UUID.randomUUID()).author(me).chat(chat)
                .status(ChatMessage.MessageStatus.SENT).build();

        when(messageRepository.save(any(ChatMessage.class))).thenReturn(saved);

        MessageResponse response = new MessageResponse();
        UserShortProfileResponse author = new UserShortProfileResponse();
        author.setId(userId);
        response.setAuthor(author);

        when(messageMapper.toResponse(any(ChatMessage.class))).thenReturn(response);
        stubChatResponseDeps();

        List<MessageResponse> result = chatService.sendMessage(chatId, request, userId);

        assertThat(result).hasSize(1);

        verify(chatRepository).updateLastActivity(chatId);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat." + chatId), any(Object.class));
        verify(mediaService).cleanupTempMedia(userId);
    }

    @Test
    void deleteMessage_notOwner_throws() {
        ChatMessage msg = ChatMessage.builder().id(UUID.randomUUID()).author(other).build();

        when(messageRepository.findById(msg.getId())).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> chatService.deleteMessage(msg.getId(), userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteMessage_owner_marksDeleted() {
        ChatMessage msg = ChatMessage.builder()
                .id(UUID.randomUUID()).author(me).status(ChatMessage.MessageStatus.SENT).build();

        when(messageRepository.findById(msg.getId())).thenReturn(Optional.of(msg));

        chatService.deleteMessage(msg.getId(), userId);

        assertThat(msg.getStatus()).isEqualTo(ChatMessage.MessageStatus.DELETED);
        verify(messageRepository).save(msg);
    }

    @Test
    void deleteMessage_missing_throws() {
        UUID id = UUID.randomUUID();
        when(messageRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteMessage(id, userId))
                .isInstanceOf(MessageNotFoundException.class);
    }

    @Test
    void editMessage_emptyContent_throws() {
        assertThatThrownBy(() -> chatService.editMessage(UUID.randomUUID(), "  ", userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editMessage_tooLong_throws() {
        String tooLong = "a".repeat(4097);
        assertThatThrownBy(() -> chatService.editMessage(UUID.randomUUID(), tooLong, userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editMessage_notOwner_throws() {
        ChatMessage msg = ChatMessage.builder().id(UUID.randomUUID()).author(other).build();
        when(messageRepository.findById(msg.getId())).thenReturn(Optional.of(msg));

        assertThatThrownBy(() -> chatService.editMessage(msg.getId(), "edited", userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void editMessage_owner_updatesContent() {
        ChatMessage msg = ChatMessage.builder()
                .id(UUID.randomUUID()).author(me).status(ChatMessage.MessageStatus.SENT).build();

        when(messageRepository.findById(msg.getId())).thenReturn(Optional.of(msg));
        when(messageRepository.save(msg)).thenReturn(msg);

        MessageResponse response = new MessageResponse();
        UserShortProfileResponse author = new UserShortProfileResponse();
        author.setId(userId);
        response.setAuthor(author);

        when(messageMapper.toResponse(msg)).thenReturn(response);

        chatService.editMessage(msg.getId(), "edited", userId);

        assertThat(msg.getContent()).isEqualTo("edited");
        assertThat(msg.getStatus()).isEqualTo(ChatMessage.MessageStatus.EDITED);
    }

    @Test
    void markMessagesAsRead_nothingUpdated_skipsNotify() {
        when(messageRepository.markMessagesAsRead(chatId, userId)).thenReturn(0);

        chatService.markMessagesAsRead(chatId, null, userId);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void markMessagesAsRead_updated_notifiesAuthor() {
        when(messageRepository.markMessagesAsRead(chatId, userId)).thenReturn(2);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        chatService.markMessagesAsRead(chatId, UUID.randomUUID(), userId);

        verify(messagingTemplate).convertAndSendToUser(eq("anna"), eq("/queue/read"), any());
    }

    @Test
    void markAllMessagesAsRead_updated_notifies() {
        when(messageRepository.markMessagesAsRead(chatId, userId)).thenReturn(1);
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        chatService.markAllMessagesAsRead(chatId, userId);

        verify(messagingTemplate).convertAndSendToUser(eq("anna"), eq("/queue/read"), any());
    }

    @Test
    void markMessagesAsReadAndReturnLast_returnsLastUnread() {
        ChatMessage last = ChatMessage.builder().id(UUID.randomUUID()).build();
        when(messageRepository.findLastUnreadMessage(chatId, userId)).thenReturn(Optional.of(last));

        ChatMessage result = chatService.markMessagesAsReadAndReturnLast(chatId, null, userId);

        assertThat(result).isEqualTo(last);
        verify(messageRepository).markMessagesAsRead(chatId, userId);
    }

    @Test
    void sendTypingStatus_nullChat_doesNothing() {
        chatService.sendTypingStatus(null, true, userId);

        verify(chatRepository, never()).findById(any());
    }

    @Test
    void sendTypingStatus_notifiesRecipient() {
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        chatService.sendTypingStatus(chatId, true, userId);

        verify(messagingTemplate).convertAndSendToUser(eq("anna"), eq("/queue/typing"), any());
    }

    @Test
    void getRecipientUsername_returnsOther() {
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        assertThat(chatService.getRecipientUsername(chatId, userId)).isEqualTo("anna");
    }

    @Test
    void getRecipientUsername_accessDenied_throws() {
        UUID stranger = UUID.randomUUID();
        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.getRecipientUsername(chatId, stranger))
                .isInstanceOf(AccessDeniedException.class);
    }
}
