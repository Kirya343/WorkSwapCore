package org.workswap.core.services.producers;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.ReviewDTO;

import lombok.RequiredArgsConstructor;

@Service
@Profile("production")
@RequiredArgsConstructor
public class ReviewProducer {

    private final AmqpTemplate amqpTemplate;

    public void reviewCreated(ReviewDTO dto) {

        amqpTemplate.convertAndSend("reviewsQueue", dto);
    }
}
