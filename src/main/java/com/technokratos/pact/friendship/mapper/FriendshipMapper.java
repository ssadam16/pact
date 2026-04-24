package com.technokratos.pact.friendship.mapper;

import com.technokratos.pact.friendship.dto.FriendshipIncomeRequest;
import com.technokratos.pact.friendship.dto.FriendshipRequest;
import com.technokratos.pact.friendship.model.Friendship;
import com.technokratos.pact.user.mapper.UserMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface FriendshipMapper {
    Friendship toFriendship(FriendshipRequest request);
    FriendshipIncomeRequest toFriendshipIncomeRequest(Friendship friendship);
}
