package com.technokratos.pact.article.model;

import com.technokratos.pact.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "article_tag")
@Inheritance(strategy = InheritanceType.JOINED)
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class ArticleTag extends BaseEntity {

    @Column(nullable = false, length = 50, unique = true)
    @Enumerated(EnumType.STRING)
    private ArticleTagName name;

    @ManyToMany(mappedBy = "tags")
    @ToString.Exclude
    private Set<Article> articles = new HashSet<>();

    public enum ArticleTagName {
        GUIDE,
        REVIEW,
        COMPARING,
        COMPLETING,
        MODS,
        BUGS,
        SETTINGS,
        SECRETS,
        THEORY,
        PLOT,
        NEWS
    }

    public ArticleTag(ArticleTagName name) {
        this.name = name;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != objectEffectiveClass) {
            return false;
        }
        ArticleTag that = (ArticleTag) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
