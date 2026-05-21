package com.technokratos.pact.chat.service;

import com.technokratos.pact.chat.dto.*;
import com.technokratos.pact.chat.exception.ChatNotFoundException;
import com.technokratos.pact.chat.exception.MessageNotFoundException;
import com.technokratos.pact.chat.mapper.ChatMapper;
import com.technokratos.pact.chat.mapper.ChatMessageMapper;
import com.technokratos.pact.chat.model.Chat;
import com.technokratos.pact.chat.model.ChatMessage;
import com.technokratos.pact.chat.model.ChatMedia;
import com.technokratos.pact.chat.repository.ChatMessageRepository;
import com.technokratos.pact.chat.repository.ChatRepository;
import com.technokratos.pact.file.service.AvatarService;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import com.technokratos.pact.user.exception.UserNotFoundException;
import com.technokratos.pact.user.model.User;
import com.technokratos.pact.user.repository.UserRepository;
import com.technokratos.pact.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatMapper chatMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatMediaService mediaService;
    private final UserService userService;
    private final AvatarService avatarService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int PAGE_SIZE = 30;
    private static final int MAX_MESSAGE_LENGTH = 4096;

    public ChatResponse createChat(ChatCreateRequest request, UUID currentUserId) {
        log.info("Creating chat between users: {} and {}", currentUserId, request.getSecondUserId());

        Optional<Chat> existingChat = chatRepository
                .findChatBetweenUsers(currentUserId, request.getSecondUserId());

        if (existingChat.isPresent()) {
            log.info("Chat already exists: {}", existingChat.get().getId());
            return getChatResponse(existingChat.get(), currentUserId);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> UserNotFoundException.byId(currentUserId));
        User secondUser = userRepository.findById(request.getSecondUserId())
                .orElseThrow(() -> UserNotFoundException.byId(request.getSecondUserId()));

        Chat chat = Chat.builder()
                .firstUser(currentUser)
                .secondUser(secondUser)
                .build();

        chat = chatRepository.save(chat);
        log.info("Chat created with id: {}", chat.getId());
        return getChatResponse(chat, currentUserId);
    }

    public Page<ChatResponse> getUserChats(UUID userId, int page) {
        log.info("Getting chats for user: {}, page: {}", userId, page);
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("updatedAt").descending());
        Page<Chat> chats = chatRepository.findAllByUserId(userId, pageable);
        return chats.map(chat -> getChatResponse(chat, userId));
    }

    public Page<MessageResponse> getChatMessages(UUID chatId, UUID userId, int page) {
        log.info("Getting messages for chat: {}, user: {}, page: {}", chatId, userId, page);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> ChatNotFoundException.byId(chatId));

        if (!chat.getFirstUser().getId().equals(userId) && !chat.getSecondUser().getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());
        Page<ChatMessage> messages = messageRepository.findByChatIdOrderByCreatedAtDesc(chatId, pageable);

        return messages.map(message -> toMessageResponse(message, userId));
    }

    public List<MessageResponse> sendMessage(UUID chatId, SendMessageRequest request, UUID currentUserId) {
        log.info("Sending message to chat: {} from user: {}", chatId, currentUserId);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> ChatNotFoundException.byId(chatId));

        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> UserNotFoundException.byId(currentUserId));

        User recipient = getChatRecipient(chat, currentUserId);

        String content = request.getContent() != null ? request.getContent() : "";
        boolean hasMedia = request.getMediaList() != null && !request.getMediaList().isEmpty();

        List<String> contentParts = splitMessageByLength(content, MAX_MESSAGE_LENGTH);
        if (contentParts.isEmpty()) {
            if (!hasMedia) {
                return new ArrayList<>();
            }
            contentParts.add("");
        }

        List<MessageResponse> sentMessages = new ArrayList<>();

        for (int i = 0; i < contentParts.size(); i++) {
            ChatMessage message = ChatMessage.builder()
                    .content(contentParts.get(i))
                    .author(author)
                    .chat(chat)
                    .status(ChatMessage.MessageStatus.SENT)
                    .isEdited(false)
                    .replyToMessage(request.getReplyToMessageId() != null ?
                            messageRepository.findById(request.getReplyToMessageId()).orElse(null) : null)
                    .build();

            message = messageRepository.save(message);

            if (hasMedia && i == contentParts.size() - 1) {
                List<ChatMedia> mediaList = mediaService.moveAndSaveMedia(request.getMediaList(), message, currentUserId);
                message.setMediaList(mediaList);
                message = messageRepository.save(message);
            }

            chatRepository.updateLastActivity(chatId);

            MessageResponse response = toMessageResponse(message, currentUserId);
            mediaService.addMediaToResponse(message, response);
            sentMessages.add(response);

            messagingTemplate.convertAndSend("/topic/chat." + chatId, response);

            messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/chats_update",
                    getChatResponse(chat, recipient.getId()));
            messagingTemplate.convertAndSendToUser(author.getUsername(), "/queue/chats_update",
                    getChatResponse(chat, author.getId()));
        }

        mediaService.cleanupTempMedia(currentUserId);

        return sentMessages;
    }

    public void deleteMessage(UUID messageId, UUID currentUserId) {
        log.info("Deleting message: {} by user: {}", messageId, currentUserId);

        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> MessageNotFoundException.byId(messageId));

        if (!message.getAuthor().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only delete your own messages");
        }

        message.setContent("Сообщение удалено");
        message.setStatus(ChatMessage.MessageStatus.DELETED);
        message.setIsEdited(true);
        messageRepository.save(message);
    }

    public MessageResponse editMessage(UUID messageId, String newContent, UUID currentUserId) {
        log.info("Editing message: {} by user: {}", messageId, currentUserId);

        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (newContent.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message too long");
        }

        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getAuthor().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only edit your own messages");
        }

        message.setContent(newContent);
        message.setStatus(ChatMessage.MessageStatus.EDITED);
        message.setIsEdited(true);
        message = messageRepository.save(message);

        return toMessageResponse(message, currentUserId);
    }

    public void markMessagesAsRead(UUID chatId, UUID messageId, UUID currentUserId) {
        log.info("Marking messages as read in chat: {} by user: {}", chatId, currentUserId);

        int updated = messageRepository.markMessagesAsRead(chatId, currentUserId);
        log.info("Marked {} messages as read", updated);
        if (updated == 0) return;

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> ChatNotFoundException.byId(chatId));
        User author = getChatRecipient(chat, currentUserId);

        Map<String, Object> readPayload = new HashMap<>();
        readPayload.put("chatId", chatId);
        readPayload.put("readBy", currentUserId);
        if (messageId != null) {
            readPayload.put("readUpToMessageId", messageId);
        }

        messagingTemplate.convertAndSendToUser(author.getUsername(), "/queue/read", readPayload);
    }

    public void markAllMessagesAsRead(UUID chatId, UUID currentUserId) {
        log.info("Marking all messages as read in chat: {} by user: {}", chatId, currentUserId);

        int updated = messageRepository.markMessagesAsRead(chatId, currentUserId);
        log.info("Marked {} messages as read", updated);
        if (updated == 0) return;

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> ChatNotFoundException.byId(chatId));
        User author = getChatRecipient(chat, currentUserId);

        Map<String, Object> readPayload = new HashMap<>();
        readPayload.put("chatId", chatId);
        readPayload.put("readBy", currentUserId);

        messagingTemplate.convertAndSendToUser(author.getUsername(), "/queue/read", readPayload);
    }

    public ChatMessage markMessagesAsReadAndReturnLast(UUID chatId, UUID messageId, UUID currentUserId) {
        ChatMessage lastUnread = messageRepository.findLastUnreadMessage(chatId, currentUserId).orElse(null);
        messageRepository.markMessagesAsRead(chatId, currentUserId);
        return lastUnread;
    }

    public void sendTypingStatus(UUID chatId, Boolean isTyping, UUID currentUserId) {
        if (chatId == null) return;

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> ChatNotFoundException.byId(chatId));

        User recipient = getChatRecipient(chat, currentUserId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("chatId", chatId);
        payload.put("userId", currentUserId);
        payload.put("isTyping", isTyping);

        messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/typing", payload);
    }

    private ChatResponse getChatResponse(Chat chat, UUID currentUserId) {
        User interlocutor = getChatRecipient(chat, currentUserId);
        UserShortProfileResponse interlocutorProfile = userService.getShortProfile(interlocutor.getId());

        Optional<ChatMessage> lastMessageOpt = messageRepository.findFirstByChatIdOrderByCreatedAtDesc(chat.getId());

        MessageShortResponse lastMessage = null;
        if (lastMessageOpt.isPresent()) {
            ChatMessage lastMsg = lastMessageOpt.get();
            lastMessage = messageMapper.toShortResponse(lastMsg);
            if (lastMsg.getAuthor() != null && lastMsg.getAuthor().getAvatarFilename() != null) {
                lastMessage.getAuthor().setAvatarUrl(avatarService.getAvatarUrl(lastMsg.getAuthor().getAvatarFilename()));
            }
        }

        long unreadCount = messageRepository.countUnreadMessages(chat.getId(), currentUserId);

        return ChatResponse.builder()
                .id(chat.getId())
                .interlocutor(interlocutorProfile)
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .build();
    }

    public String getRecipientUsername(UUID chatId, UUID currentUserId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> ChatNotFoundException.byId(chatId));

        if (!chat.getFirstUser().getId().equals(currentUserId)
                && !chat.getSecondUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }

        return getChatRecipient(chat, currentUserId).getUsername();
    }

    private User getChatRecipient(Chat chat, UUID currentUserId) {
        if (chat.getFirstUser().getId().equals(currentUserId)) {
            return chat.getSecondUser();
        } else {
            return chat.getFirstUser();
        }
    }

    private MessageResponse toMessageResponse(ChatMessage message, UUID currentUserId) {
        MessageResponse response = messageMapper.toResponse(message);

        if (message.getAuthor() != null && message.getAuthor().getAvatarFilename() != null) {
            response.getAuthor().setAvatarUrl(avatarService.getAvatarUrl(message.getAuthor().getAvatarFilename()));
        }

        if (message.getReplyToMessage() != null) {
            response.setReplyToMessage(messageMapper.toShortResponse(message.getReplyToMessage()));
        }

        if (currentUserId.equals(message.getAuthor().getId())) {
            response.setStatus(message.getReadAt() != null
                    ? ChatMessage.MessageStatus.READ
                    : ChatMessage.MessageStatus.SENT);
        } else {
            response.setStatus(message.getStatus());
        }

        return response;
    }

    private List<String> splitMessageByLength(String content, int maxLength) {
        List<String> parts = new ArrayList<>();
        if (content == null || content.isEmpty()) return parts;
        if (content.length() <= maxLength) {
            parts.add(content);
            return parts;
        }
        for (int i = 0; i < content.length(); i += maxLength) {
            parts.add(content.substring(i, Math.min(content.length(), i + maxLength)));
        }
        return parts;
    }
}