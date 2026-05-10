package com.technokratos.pact.article.mapper;

import com.technokratos.pact.article.dto.CommentResponse;
import com.technokratos.pact.article.model.Comment;
import com.technokratos.pact.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ArticleMapper.class})
public interface CommentMapper {
    CommentResponse toCommentResponse(Comment comment);

    default Page<CommentResponse> toCommentResponsePage(Page<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return null;
        }
        return comments.map(this::toCommentResponse);
    }
}
