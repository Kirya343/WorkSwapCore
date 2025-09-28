package org.workswap.core.services.producers;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.stat.OnlineStatSnapshotDTO;

import lombok.RequiredArgsConstructor;

@Service
@Profile("production")
@RequiredArgsConstructor
public class OnlineStatProducer {

    private final AmqpTemplate amqpTemplate;

    public void sendOnlineStat(OnlineStatSnapshotDTO dto) {

        amqpTemplate.convertAndSend("onlineStatQueue", dto);
    }
}

