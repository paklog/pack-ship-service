package com.paklog.wes.pack.infrastructure.events;

import com.paklog.wes.pack.application.service.PackingSessionService;
import com.paklog.wes.pack.domain.aggregate.PackingSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Event handler for Picking events
 * Listens to picking-related events and creates packing sessions
 * TODO: Adapt to use PackingSessionService with proper commands
 */
@Component
public class PickEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(PickEventHandler.class);

    private final PackingSessionService packingSessionService;

    public PickEventHandler(PackingSessionService packingSessionService) {
        this.packingSessionService = packingSessionService;
    }

    /**
     * Handle PickingCompletedEvent from pick-execution-service
     * Creates packing sessions for completed picks
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.pick-events:wes-pick-events}",
            groupId = "${paklog.kafka.consumer.group-id:pack-ship-service}"
    )
    public void handlePickingCompleted(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"PickingCompletedEvent".equals(eventType)) {
                return; // Ignore other event types
            }

            logger.info("Received PickingCompletedEvent: {}", eventData);

            String sessionId = (String) eventData.get("sessionId");
            String orderId = (String) eventData.get("orderId");
            String warehouseId = (String) eventData.get("warehouseId");
            String completedBy = (String) eventData.get("completedBy");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>)
                    eventData.getOrDefault("items", List.of());

            // Create packing session for the completed pick
            createPackingSession(orderId, warehouseId, sessionId, items);

            logger.info("Created packing session for order {} from pick session {}",
                    orderId, sessionId);

        } catch (Exception e) {
            logger.error("Error handling PickingCompletedEvent", e);
            // In production, publish to dead letter queue
        }
    }

    private void createPackingSession(String orderId, String warehouseId,
                                      String pickSessionId, List<Map<String, Object>> items) {
        // TODO: Implement using StartPackingSessionCommand
        logger.warn("createPackingSession needs implementation with proper command pattern");
        logger.debug("Would create packing session for order {} with {} items", orderId, items.size());
    }

    /**
     * Handle PickShortageEvent from pick-execution-service
     * Handles pick shortages by creating partial packing sessions
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.pick-events:wes-pick-events}",
            groupId = "${paklog.kafka.consumer.group-id:pack-ship-service}"
    )
    public void handlePickShortage(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"PickShortageEvent".equals(eventType)) {
                return;
            }

            logger.warn("Received PickShortageEvent: {}", eventData);

            String orderId = (String) eventData.get("orderId");
            String sku = (String) eventData.get("sku");
            Integer expectedQty = ((Number) eventData.get("expectedQuantity")).intValue();
            Integer pickedQty = ((Number) eventData.get("pickedQuantity")).intValue();
            Integer shortQty = expectedQty - pickedQty;

            logger.warn("Pick shortage for order {}: SKU {} short {} units",
                    orderId, sku, shortQty);

            // In a real system, this might trigger:
            // 1. Notification to warehouse manager
            // 2. Inventory adjustment
            // 3. Customer notification
            // 4. Backorder creation

        } catch (Exception e) {
            logger.error("Error handling PickShortageEvent", e);
        

}
}
}
