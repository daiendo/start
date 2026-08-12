package com.daiend.muriox.auth;

import java.util.List;
import java.util.Objects;

public record UserAuthorityChangedEvent(
        List<Long> userIds
) {
    public UserAuthorityChangedEvent {
        Objects.requireNonNull(
                userIds,
                "受影响用户ID列表不能为空");

        userIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
