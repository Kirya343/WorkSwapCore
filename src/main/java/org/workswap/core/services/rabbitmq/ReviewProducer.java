package org.workswap.core.services.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.workswap.common.dto.ReviewDTO;

import lombok.RequiredArgsConstructor;

@Service
@Profile("production")
@RequiredArgsConstructor
public class ReviewProducer {

    private static final Logger logger = LoggerFactory.getLogger(ReviewProducer.class);
    private final AmqpTemplate amqpTemplate;

    public void reviewCreated(Long listingId, Long profileId) {
        System.out.println("Вошли в reviewCreated");
        System.out.flush();

        ReviewDTO dto = new ReviewDTO();
        dto.setProfileId(profileId);
        dto.setListingId(listingId);

        System.out.println("DTO сформирован: " + dto);
        System.out.flush();

        amqpTemplate.convertAndSend("reviewsQueue", dto);
        System.out.println("Сообщение отправлено в очередь");
        System.out.flush();
    }
}
