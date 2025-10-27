package com.paklog.wes.pack.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wes.pack.integration.contracts.PickingCompletedContract;
import io.cloudevents.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PickingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PickingEventConsumer.class);

    private final ObjectMapper objectMapper;

    public PickingEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "picking-events", groupId = "pack-ship-service")
    public void handlePickingEvent(CloudEvent cloudEvent) {
        String eventType = cloudEvent.getType();
        log.info("Received event: type={}", eventType);

        try {
            if (PickingCompletedContract.EVENT_TYPE.equals(eventType)) {
                handlePickingCompleted(cloudEvent);
            }
        } catch (Exception e) {
            log.error("Failed to handle picking event: type={}", eventType, e);
            throw new RuntimeException("Failed to handle picking event", e);
        }
    }

    private void handlePickingCompleted(CloudEvent cloudEvent) throws Exception {
        PickingCompletedContract contract = objectMapper.readValue(
            cloudEvent.getData().toBytes(),
            PickingCompletedContract.class
        );

        log.info("Picking completed - creating pack task: pickTaskId={}, items={}",
            contract.pickTaskId(), contract.itemsPicked());

        // TODO: Implement pack task creation logic
        // This would call a use case to create a pack task from the picking event
    }
}
