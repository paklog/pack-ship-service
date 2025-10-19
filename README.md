# Pack and Ship Service (WES)

Packing operations, quality control, and shipping preparation service.

## Responsibilities

- Packing station operations
- Quality inspection workflows
- Carton selection
- Weight verification
- Shipping label generation coordination
- Packing material management

## Architecture

```
domain/
├── aggregate/      # PackingSession, QualityCheck
├── entity/         # ItemToScan, PackedCarton
├── valueobject/    # PackingStatus, QualityCheckResult, Weight
├── service/        # PackingSessionService, QualityControlService
├── repository/     # PackingSessionRepository
└── event/          # PackingStartedEvent, PackingCompletedEvent

application/
├── command/        # StartPackingCommand, ScanItemCommand
├── query/          # GetPackingSessionQuery, GetQualityMetricsQuery
└── handler/        # PickCompletedHandler

adapter/
├── rest/           # Packing station controllers
└── persistence/    # MongoDB repositories

infrastructure/
├── config/         # Spring configurations
├── messaging/      # Kafka publishers/consumers
└── events/         # Event publishing infrastructure
```

## Tech Stack

- Java 21
- Spring Boot 3.2.0
- MongoDB (packing session data)
- Apache Kafka (event-driven integration)
- CloudEvents
- OpenAPI/Swagger

## Running the Service

```bash
mvn spring-boot:run
```

## API Documentation

Available at: http://localhost:8084/swagger-ui.html

## Events Published

- `PackingSessionStartedEvent` - When packing begins
- `ItemScannedEvent` - When an item is scanned
- `QualityCheckCompletedEvent` - When quality check is done
- `PackingCompletedEvent` - When order is packed
- `PackingFailedEvent` - When packing fails quality check

## Events Consumed

- `PickSessionCompletedEvent` - From Pick Execution Service
- `TaskAssignedEvent` - From Task Execution Service
