package com.technokratos.pact.chat.model;

import com.technokratos.pact.common.BaseEntity;
import com.technokratos.pact.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "chat")
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Chat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "f_user_id", nullable = false)
    @ToString.Exclude
    private User firstUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "s_user_id", nullable = false)
    @ToString.Exclude
    private User secondUser;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy ?
                proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ?
                proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != objectEffectiveClass) return false;
        Chat chat = (Chat) o;
        return getId() != null && Objects.equals(getId(), chat.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ?
                proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}