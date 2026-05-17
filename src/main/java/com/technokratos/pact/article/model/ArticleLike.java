package com.technokratos.pact.article.model;

import com.technokratos.pact.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "article_like")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder(toBuilder = true)
public class ArticleLike implements Serializable {

    @EmbeddedId
    private ArticleLikeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("articleId")
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleLikeId implements Serializable {

        @Column(name = "user_id")
        private UUID userId;

        @Column(name = "article_id")
        private UUID articleId;
    }
}
