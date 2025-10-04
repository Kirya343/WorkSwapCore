package org.workswap.core.services.producers;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.stat.UsersStatSnapshotDTO;

import lombok.RequiredArgsConstructor;

@Service
@Profile("production")
@RequiredArgsConstructor
public class UsersStatProducer {

    private final AmqpTemplate amqpTemplate;

    public void sendUsersStat(UsersStatSnapshotDTO dto) {

        amqpTemplate.convertAndSend("usersStatQueue", dto);
    }
}

