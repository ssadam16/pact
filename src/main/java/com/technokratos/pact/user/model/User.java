package com.technokratos.pact.user.model;

import com.technokratos.pact.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "user_entity")
@Inheritance(strategy = InheritanceType.JOINED)
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String hashPassword;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    private String providerId;

    private String avatarFilename;

    @Column(length = 50)
    private String steamId;

    @ColumnDefault("true")
    private boolean isEnabled;

    @ColumnDefault("false")
    private boolean isVerified;

    public enum Role {
        USER,
        MODER,
        ADMIN
    }

    public enum AuthProvider {
        LOCAL,
        GOOGLE,
        GITHUB,
        VK,
        YANDEX
    }
}