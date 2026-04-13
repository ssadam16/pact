package com.technokratos.pact.user.mapper;

import com.technokratos.pact.user.dto.UserProfileResponse;
import com.technokratos.pact.user.dto.UserShortProfileResponse;
import com.technokratos.pact.user.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserProfileResponse toUserProfileResponse(User user);
    UserShortProfileResponse toUserShortProfileResponse(User user);
}
