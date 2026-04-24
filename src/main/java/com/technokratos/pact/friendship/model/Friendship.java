package com.technokratos.pact.friendship.model;

import com.technokratos.pact.common.BaseEntity;
import com.technokratos.pact.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "friendship")
@Inheritance(strategy = InheritanceType.JOINED)
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Friendship extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "who_id", nullable = false)
    private User who;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_whom_id", nullable = false)
    private User toWhom;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;

    public enum FriendshipStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}
