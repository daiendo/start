package com.daiend.muriox.auth;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class UserAuthorityChangePublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserAuthorityChangePublisher(
            ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishForUser(Long userId) {
        publishForUsers(List.of(userId));
    }

    public void publishForUsers(
            Collection<Long> userIds) {

        UserAuthorityChangedEvent event =
                new UserAuthorityChangedEvent(
                        userIds.stream().toList());

        if (event.userIds().isEmpty()) {
            return;
        }

        applicationEventPublisher.publishEvent(event);
    }
}
