# Pack & Ship Service - Decoupling Plan
## Eliminate Dependencies on Shared Modules

**Service:** pack-ship-service
**Complexity:** LOW (only domain primitives)
**Estimated Effort:** 3 hours
**Priority:** Phase 4 (after pick-execution-service)

---

## Current Dependencies

### Shared Modules Used
- ✓ **paklog-domain** (v0.0.1-SNAPSHOT)
  - `com.paklog.domain.annotation.AggregateRoot`
  - `com.paklog.domain.shared.DomainEvent`
  - `com.paklog.domain.valueobject.Priority`

### Coupling Impact
- Cannot deploy independently
- Breaking changes in shared modules affect this service
- Build requires paklog-domain artifact
- Testing requires paklog-domain dependency

---

## Target Architecture

### Service-Owned Components
```
pack-ship-service/
├── src/main/java/com/paklog/pack/ship/
│   ├── domain/
│   │   ├── shared/
│   │   │   ├── AggregateRoot.java        # Copy from paklog-domain
│   │   │   └── DomainEvent.java          # Copy from paklog-domain
│   │   │
│   │   └── valueobject/
│   │       └── Priority.java             # Copy from paklog-domain
│   │
│   ├── events/                            # Publisher-owned schemas
│   │   ├── packing/
│   │   │   ├── PackingStartedEvent.java
│   │   │   ├── PackingCompletedEvent.java
│   │   │   └── QualityCheckCompletedEvent.java
│   │   │
│   │   └── shipping/
│   │       ├── ShippingLabelGeneratedEvent.java
│   │       ├── CartonOptimizedEvent.java
│   │       └── ShipmentReadyEvent.java
│   │
│   ├── integration/
│   │   └── contracts/                     # Consumer contracts
│   │       └── PickingCompletedContract.java  # From pick-execution
│   │
│   └── infrastructure/
│       └── events/
│           ├── PackShipEventPublisher.java
│           └── PickingEventConsumer.java
```

---

## CloudEvents Schema Definition

### Event Type Pattern
All events MUST follow:
- Packing: `com.paklog.wes.pack-ship.packing.<entity>.<action>`
- Shipping: `com.paklog.wes.pack-ship.shipping.<entity>.<action>`

### Events Published by Pack & Ship Service

#### 1. Packing Started Event
**Type:** `com.paklog.wes.pack-ship.packing.pack-task.started.v1`
**Trigger:** Worker starts packing items
**Schema:**
```json
{
  "pack_task_id": "PACK-12345",
  "pick_task_id": "PICK-001",
  "wave_id": "WAVE-001",
  "order_id": "ORD-123",
  "worker_id": "WORKER-789",
  "items_to_pack": 15,
  "started_at": "2025-10-26T10:20:00Z",
  "packing_station": "PACK-ST-05"
}
```

#### 2. Packing Completed Event
**Type:** `com.paklog.wes.pack-ship.packing.pack-task.completed.v1`
**Trigger:** All items packed successfully
**Schema:**
```json
{
  "pack_task_id": "PACK-12345",
  "order_id": "ORD-123",
  "wave_id": "WAVE-001",
  "worker_id": "WORKER-789",
  "completed_at": "2025-10-26T10:35:00Z",
  "duration_seconds": 900,
  "items_packed": 15,
  "cartons_used": 2,
  "total_weight_kg": 5.4,
  "total_volume_cubic_meters": 0.08
}
```

#### 3. Quality Check Completed Event
**Type:** `com.paklog.wes.pack-ship.packing.quality-check.completed.v1`
**Trigger:** Quality check passed/failed
**Schema:**
```json
{
  "quality_check_id": "QC-12345",
  "pack_task_id": "PACK-12345",
  "order_id": "ORD-123",
  "check_type": "VISUAL|WEIGHT|SCAN",
  "result": "PASS|FAIL",
  "checked_by": "WORKER-789",
  "checked_at": "2025-10-26T10:36:00Z",
  "issues_found": []
}
```

#### 4. Shipping Label Generated Event
**Type:** `com.paklog.wes.pack-ship.shipping.label.generated.v1`
**Trigger:** Shipping label created for carton
**Schema:**
```json
{
  "label_id": "LABEL-12345",
  "tracking_number": "1Z999AA10123456784",
  "carton_id": "CARTON-001",
  "order_id": "ORD-123",
  "carrier": "UPS|FEDEX|USPS|DHL",
  "service_level": "GROUND|2DAY|OVERNIGHT",
  "generated_at": "2025-10-26T10:37:00Z",
  "estimated_delivery_date": "2025-10-28"
}
```

#### 5. Carton Optimized Event
**Type:** `com.paklog.wes.pack-ship.packing.carton.optimized.v1`
**Trigger:** 3D bin packing optimization completed
**Schema:**
```json
{
  "optimization_id": "OPT-12345",
  "order_id": "ORD-123",
  "items_count": 15,
  "cartons_required": 2,
  "total_volume_cubic_meters": 0.08,
  "space_utilization_percentage": 85.5,
  "algorithm_used": "3D_BIN_PACKING",
  "optimization_time_ms": 125,
  "optimized_at": "2025-10-26T10:19:55Z"
}
```

#### 6. Shipment Ready Event
**Type:** `com.paklog.wes.pack-ship.shipping.shipment.ready.v1`
**Trigger:** Shipment ready for carrier pickup
**Schema:**
```json
{
  "shipment_id": "SHIP-12345",
  "order_id": "ORD-123",
  "wave_id": "WAVE-001",
  "tracking_numbers": ["1Z999AA10123456784", "1Z999AA10123456785"],
  "carton_count": 2,
  "total_weight_kg": 5.4,
  "carrier": "UPS",
  "service_level": "GROUND",
  "ready_at": "2025-10-26T10:40:00Z",
  "pickup_scheduled_at": "2025-10-26T16:00:00Z"
}
```

### Events Consumed by Pack & Ship Service

#### Picking Completed Event (from pick-execution-service)
**Type:** `com.paklog.wes.pick-execution.picking.pick-task.completed.v1`
**Purpose:** Creates pack task when picking is completed
**Contract:**
```json
{
  "pick_task_id": "PICK-12345",
  "task_id": "TASK-001",
  "wave_id": "WAVE-001",
  "worker_id": "WORKER-456",
  "completed_at": "2025-10-26T10:15:00Z",
  "items_picked": 15
}
```

---

## Step-by-Step Migration Tasks

### Phase 1: Preparation (15 minutes)

#### Task 1.1: Create Feature Branch
```bash
cd pack-ship-service
git checkout -b decouple/remove-shared-dependencies
```

#### Task 1.2: Run Baseline Tests
```bash
mvn clean test
```

#### Task 1.3: Create Service-Internal Packages
```bash
mkdir -p src/main/java/com/paklog/pack/ship/domain/shared
mkdir -p src/main/java/com/paklog/pack/ship/domain/valueobject
mkdir -p src/main/java/com/paklog/pack/ship/events/packing
mkdir -p src/main/java/com/paklog/pack/ship/events/shipping
mkdir -p src/main/java/com/paklog/pack/ship/integration/contracts
mkdir -p src/main/java/com/paklog/pack/ship/infrastructure/events
```

---

### Phase 2: Internalize Domain Primitives (1 hour)

#### Task 2.1: Copy AggregateRoot Annotation
```bash
cp ../../paklog-domain/src/main/java/com/paklog/domain/annotation/AggregateRoot.java \
   src/main/java/com/paklog/pack/ship/domain/shared/AggregateRoot.java
```

**Update package:**
```java
// File: src/main/java/com/paklog/pack/ship/domain/shared/AggregateRoot.java
package com.paklog.pack.ship.domain.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for aggregate roots in Pack & Ship bounded context
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregateRoot {
}
```

#### Task 2.2: Copy DomainEvent Interface
```bash
cp ../../paklog-domain/src/main/java/com/paklog/domain/shared/DomainEvent.java \
   src/main/java/com/paklog/pack/ship/domain/shared/DomainEvent.java
```

**Update package:**
```java
// File: src/main/java/com/paklog/pack/ship/domain/shared/DomainEvent.java
package com.paklog.pack.ship.domain.shared;

import java.time.Instant;

/**
 * Base interface for all domain events in Pack & Ship bounded context
 */
public interface DomainEvent {
    Instant occurredOn();
    String eventType();
}
```

#### Task 2.3: Copy Priority Value Object
```bash
cp ../../paklog-domain/src/main/java/com/paklog/domain/valueobject/Priority.java \
   src/main/java/com/paklog/pack/ship/domain/valueobject/Priority.java
```

**Update package:**
```java
// File: src/main/java/com/paklog/pack/ship/domain/valueobject/Priority.java
package com.paklog.pack.ship.domain.valueobject;

public enum Priority {
    URGENT,   // Expedited shipment
    HIGH,     // High priority
    NORMAL,   // Standard
    LOW;      // Low priority

    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return Priority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
```

#### Task 2.4: Update All Imports
```bash
# Update imports in source files
find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.annotation\.AggregateRoot/import com.paklog.pack.ship.domain.shared.AggregateRoot/g' {} +

find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.shared\.DomainEvent/import com.paklog.pack.ship.domain.shared.DomainEvent/g' {} +

find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.valueobject\.Priority/import com.paklog.pack.ship.domain.valueobject.Priority/g' {} +

# Update test files
find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.annotation\.AggregateRoot/import com.paklog.pack.ship.domain.shared.AggregateRoot/g' {} +

find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.shared\.DomainEvent/import com.paklog.pack.ship.domain.shared.DomainEvent/g' {} +

find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.valueobject\.Priority/import com.paklog.pack.ship.domain.valueobject.Priority/g' {} +
```

#### Task 2.5: Remove paklog-domain Dependency
**Edit pom.xml:**
```xml
<!-- DELETE THIS DEPENDENCY -->
<!--
<dependency>
    <groupId>com.paklog.common</groupId>
    <artifactId>paklog-domain</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
-->
```

#### Task 2.6: Verify Compilation
```bash
mvn clean compile
```

---

### Phase 3: Define Event Schemas (1.5 hours)

#### Task 3.1: Create Packing Events

**File:** `src/main/java/com/paklog/pack/ship/events/packing/PackingStartedEvent.java`
```java
package com.paklog.pack.ship.events.packing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.packing.pack-task.started.v1
 */
public record PackingStartedEvent(
    @JsonProperty("pack_task_id") String packTaskId,
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("items_to_pack") int itemsToPack,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("packing_station") String packingStation
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.packing.pack-task.started.v1";
}
```

**File:** `src/main/java/com/paklog/pack/ship/events/packing/PackingCompletedEvent.java`
```java
package com.paklog.pack.ship.events.packing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.packing.pack-task.completed.v1
 */
public record PackingCompletedEvent(
    @JsonProperty("pack_task_id") String packTaskId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("duration_seconds") long durationSeconds,
    @JsonProperty("items_packed") int itemsPacked,
    @JsonProperty("cartons_used") int cartonsUsed,
    @JsonProperty("total_weight_kg") double totalWeightKg,
    @JsonProperty("total_volume_cubic_meters") double totalVolumeCubicMeters
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.packing.pack-task.completed.v1";
}
```

**File:** `src/main/java/com/paklog/pack/ship/events/packing/QualityCheckCompletedEvent.java`
```java
package com.paklog.pack.ship.events.packing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.packing.quality-check.completed.v1
 */
public record QualityCheckCompletedEvent(
    @JsonProperty("quality_check_id") String qualityCheckId,
    @JsonProperty("pack_task_id") String packTaskId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("check_type") String checkType,
    @JsonProperty("result") String result,
    @JsonProperty("checked_by") String checkedBy,
    @JsonProperty("checked_at") Instant checkedAt,
    @JsonProperty("issues_found") List<String> issuesFound
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.packing.quality-check.completed.v1";
}
```

**File:** `src/main/java/com/paklog/pack/ship/events/packing/CartonOptimizedEvent.java`
```java
package com.paklog.pack.ship.events.packing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.packing.carton.optimized.v1
 */
public record CartonOptimizedEvent(
    @JsonProperty("optimization_id") String optimizationId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("items_count") int itemsCount,
    @JsonProperty("cartons_required") int cartonsRequired,
    @JsonProperty("total_volume_cubic_meters") double totalVolumeCubicMeters,
    @JsonProperty("space_utilization_percentage") double spaceUtilizationPercentage,
    @JsonProperty("algorithm_used") String algorithmUsed,
    @JsonProperty("optimization_time_ms") long optimizationTimeMs,
    @JsonProperty("optimized_at") Instant optimizedAt
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.packing.carton.optimized.v1";
}
```

#### Task 3.2: Create Shipping Events

**File:** `src/main/java/com/paklog/pack/ship/events/shipping/ShippingLabelGeneratedEvent.java`
```java
package com.paklog.pack.ship.events.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.shipping.label.generated.v1
 */
public record ShippingLabelGeneratedEvent(
    @JsonProperty("label_id") String labelId,
    @JsonProperty("tracking_number") String trackingNumber,
    @JsonProperty("carton_id") String cartonId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("carrier") String carrier,
    @JsonProperty("service_level") String serviceLevel,
    @JsonProperty("generated_at") Instant generatedAt,
    @JsonProperty("estimated_delivery_date") LocalDate estimatedDeliveryDate
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.shipping.label.generated.v1";
}
```

**File:** `src/main/java/com/paklog/pack/ship/events/shipping/ShipmentReadyEvent.java`
```java
package com.paklog.pack.ship.events.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * CloudEvent Type: com.paklog.wes.pack-ship.shipping.shipment.ready.v1
 */
public record ShipmentReadyEvent(
    @JsonProperty("shipment_id") String shipmentId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("tracking_numbers") List<String> trackingNumbers,
    @JsonProperty("carton_count") int cartonCount,
    @JsonProperty("total_weight_kg") double totalWeightKg,
    @JsonProperty("carrier") String carrier,
    @JsonProperty("service_level") String serviceLevel,
    @JsonProperty("ready_at") Instant readyAt,
    @JsonProperty("pickup_scheduled_at") Instant pickupScheduledAt
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pack-ship.shipping.shipment.ready.v1";
}
```

#### Task 3.3: Create Picking Completed Contract

**File:** `src/main/java/com/paklog/pack/ship/integration/contracts/PickingCompletedContract.java`
```java
package com.paklog.pack.ship.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Contract for PickingCompletedEvent from pick-execution-service
 * Anti-Corruption Layer
 */
public record PickingCompletedContract(
    @JsonProperty("pick_task_id") String pickTaskId,
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("items_picked") int itemsPicked
) {
    public static final String EVENT_TYPE = "com.paklog.wes.pick-execution.picking.pick-task.completed.v1";
}
```

#### Task 3.4: Create CloudEvents Publisher

**File:** `src/main/java/com/paklog/pack/ship/infrastructure/events/PackShipEventPublisher.java`
```java
package com.paklog.pack.ship.infrastructure.events;

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
```

#### Task 3.5: Create Picking Event Consumer

**File:** `src/main/java/com/paklog/pack/ship/infrastructure/events/PickingEventConsumer.java`
```java
package com.paklog.pack.ship.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.pack.ship.application.usecases.CreatePackTaskFromPickingUseCase;
import com.paklog.pack.ship.integration.contracts.PickingCompletedContract;
import io.cloudevents.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PickingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PickingEventConsumer.class);

    private final CreatePackTaskFromPickingUseCase createPackTaskUseCase;
    private final ObjectMapper objectMapper;

    public PickingEventConsumer(
            CreatePackTaskFromPickingUseCase createPackTaskUseCase,
            ObjectMapper objectMapper) {
        this.createPackTaskUseCase = createPackTaskUseCase;
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
            throw e;
        }
    }

    private void handlePickingCompleted(CloudEvent cloudEvent) throws Exception {
        PickingCompletedContract contract = objectMapper.readValue(
            cloudEvent.getData().toBytes(),
            PickingCompletedContract.class
        );

        log.info("Creating pack task from picking: pickTaskId={}, items={}",
            contract.pickTaskId(), contract.itemsPicked());

        createPackTaskUseCase.execute(
            contract.pickTaskId(),
            contract.waveId(),
            contract.itemsPicked()
        );
    }
}
```

---

### Phase 4: Testing & Validation (30 minutes)

#### Task 4.1: Run Tests
```bash
mvn clean verify
```

#### Task 4.2: Build Independently
```bash
mvn clean package
```

#### Task 4.3: Run Locally
```bash
mvn spring-boot:run
```

---

## Validation Checklist

- [ ] No compilation errors
- [ ] All tests passing
- [ ] Service builds independently
- [ ] CloudEvents published correctly
- [ ] Picking events consumed
- [ ] No paklog-domain references

---

## Success Criteria

- ✅ Zero dependencies on paklog-domain
- ✅ Independent deployment
- ✅ All tests passing
- ✅ Production ready

---

**Estimated Total Time:** 3 hours
**Complexity:** LOW
**Risk Level:** LOW
