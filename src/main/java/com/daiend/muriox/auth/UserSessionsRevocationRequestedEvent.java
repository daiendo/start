package com.daiend.muriox.auth;

import java.util.List;

public record UserSessionsRevocationRequestedEvent(
        List<Long> userIds
) {
}