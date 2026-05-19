package com.technokratos.pact.chat.model;

import com.technokratos.pact.common.BaseEntity;
import com.technokratos.pact.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "chat_message")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class ChatMessage extends BaseEntity {

    @Column(length = 4096)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    @ToString.Exclude
    private Chat chat;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<ChatMedia> mediaList = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    private LocalDateTime readAt;

    private Boolean isEdited;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    @ToString.Exclude
    private ChatMessage replyToMessage;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy ?
                proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ?
                proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != objectEffectiveClass) return false;
        ChatMessage that = (ChatMessage) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ?
                proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    public enum MessageStatus {
        SENT,
        READ,
        DELETED,
        EDITED,
        FAILED
    }
}