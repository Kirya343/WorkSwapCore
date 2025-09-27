package org.workswap.core.services.producers;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.ReviewDTO;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@Profile("production")
@RequiredArgsConstructor
public class ReviewProducer {

    private final AmqpTemplate amqpTemplate;

    @PostConstruct
    public void checkTemplate() {
        if (amqpTemplate instanceof RabbitTemplate rabbitTemplate) {
            System.out.println("ReviewProducer использует конвертер: " 
                + rabbitTemplate.getMessageConverter().getClass().getName());
        } else {
            System.out.println("ReviewProducer использует " + amqpTemplate.getClass());
        }
    }

    public void reviewCreated(ReviewDTO dto) {

        amqpTemplate.convertAndSend("reviewsQueue", dto);
    }
}
