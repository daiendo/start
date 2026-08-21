package com.daiend.muriox.auth;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class UserSessionRevocationPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public UserSessionRevocationPublisher(
            ApplicationEventPublisher eventPublisher) {

        this.eventPublisher = eventPublisher;
    }

    public void publishForUser(Long userId) {
        if (userId == null) {
            return;
        }
        publishForUsers(List.of(userId));
    }

    public void publishForUsers(
            Collection<Long> userIds) {
        List<Long> normalizedUserIds =
                userIds.stream()
                        .filter(id -> id != null)
                        .distinct()
                        .toList();

        if (normalizedUserIds.isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(
                new UserSessionsRevocationRequestedEvent(
                        normalizedUserIds));
    }
}