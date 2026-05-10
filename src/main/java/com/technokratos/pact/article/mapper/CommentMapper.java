package com.technokratos.pact.article.mapper;

import com.technokratos.pact.article.dto.CommentResponse;
import com.technokratos.pact.article.model.Comment;
import com.technokratos.pact.user.mapper.UserMapper;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class, ArticleMapper.class})
public interface CommentMapper {
    CommentResponse toCommentResponse(Comment comment);
    List<CommentResponse> toCommentResponseList(List<Comment> comments);
}
