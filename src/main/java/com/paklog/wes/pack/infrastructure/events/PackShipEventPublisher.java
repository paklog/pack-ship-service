package com.paklog.wes.pack.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PackShipEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PackShipEventPublisher.class);
    private static final String SOURCE = "paklog://pack-ship-service";

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PackShipEventPublisher(KafkaTemplate<String, CloudEvent> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, String key, String eventType, Object eventData) {
        try {
            CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create(SOURCE))
                .withType(eventType)
                .withDataContentType("application/json")
                .withTime(OffsetDateTime.now())
                .withData(objectMapper.writeValueAsBytes(eventData))
                .build();

            kafkaTemplate.send(topic, key, cloudEvent);
            log.info("Event published: type={}, key={}", eventType, key);
        } catch (Exception e) {
            log.error("Failed to publish event: type={}, key={}", eventType, key, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
