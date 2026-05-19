package com.technokratos.pact.chat.model;

import com.technokratos.pact.common.BaseEntity;
import com.technokratos.pact.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_read_receipt")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadReceipt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @Column(nullable = false)
    private LocalDateTime readAt;
}