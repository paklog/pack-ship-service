# Pack & Ship Service

Intelligent packing operations with 3D bin packing algorithms, automated carton selection, quality control, and shipping preparation workflows.

## Overview

The Pack & Ship Service manages the complete packing and shipping preparation lifecycle within warehouse operations. This bounded context receives picked items, orchestrates scan-and-pack workflows, performs intelligent carton selection using 3D bin packing algorithms, validates weight and dimensions, executes multi-point quality inspections, generates shipping labels, and prepares packages for carrier pickup. The service integrates with packing stations, scales, label printers, and carrier systems to provide seamless pack-to-ship operations.

## Domain-Driven Design

### Bounded Context
**Packing & Shipping Preparation** - Manages packing operations from item scanning through shipment creation with intelligent carton optimization and quality control.

### Core Domain Model

#### Aggregates
- **PackingSession** - Root aggregate representing a packing workflow for an order

#### Entities
- **PackingInstruction** - Individual item to be packed
- **Container** - Physical carton/box used for packing
- **QualityCheck** - Multi-point inspection record
- **PackingMaterial** - Protective materials used
- **ItemToScan** - Item requiring barcode scan
- **ScannedItem** - Verified scanned item

#### Value Objects
- **PackingStatus** - Session status (CREATED, SCANNING, READY_FOR_CARTON, PACKING, READY_TO_WEIGH, QC_PASSED, READY_TO_SHIP, COMPLETED, CANCELLED)
- **Weight** - Package weight with unit conversion
- **ContainerType** - Carton types (SMALL_BOX, MEDIUM_BOX, LARGE_BOX, POLY_MAILER, CUSTOM)
- **QualityCheckResult** - Pass/fail with checkpoint details

#### Domain Events
- **PackingSessionStartedEvent** - Packing session initiated
- **ItemScannedEvent** - Item barcode scanned successfully
- **ItemPackedEvent** - Item placed in container
- **ContainerSealedEvent** - Container sealed and weighed
- **QualityCheckCompletedEvent** - Quality inspection completed
- **PackingSessionCompletedEvent** - All packing finished
- **ShipmentCreatedEvent** - Shipping label generated

### Ubiquitous Language
- **Packing Session**: Complete pack workflow for picked items
- **Scan-and-Pack**: Barcode validation before packing
- **Carton Selection**: Choosing optimal box size
- **3D Bin Packing**: Volumetric optimization algorithm
- **Weight Verification**: Actual vs expected weight validation
- **Quality Checkpoint**: Specific inspection criteria
- **Packing Material**: Protective supplies (bubble wrap, paper, tape)
- **Dimensional Weight**: Volumetric weight for shipping cost
- **Tare Weight**: Empty container weight

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

```
src/main/java/com/paklog/wes/pack/
├── domain/                           # Core business logic
│   ├── aggregate/                   # Aggregates
│   │   └── PackingSession.java      # Session aggregate root
│   ├── entity/                      # Entities
│   │   ├── PackingInstruction.java
│   │   ├── Container.java
│   │   ├── QualityCheck.java
│   │   ├── PackingMaterial.java
│   │   ├── ItemToScan.java
│   │   └── ScannedItem.java
│   ├── valueobject/                 # Value objects
│   │   ├── PackingStatus.java
│   │   ├── Weight.java
│   │   ├── ContainerType.java
│   │   └── QualityCheckResult.java
│   ├── repository/                  # Repository interfaces
│   │   └── PackingSessionRepository.java
│   ├── service/                     # Domain services
│   │   ├── BinPackingOptimizer.java
│   │   └── CartonSelector.java
│   └── event/                       # Domain events
├── application/                      # Use cases & orchestration
│   ├── service/                     # Application services
│   │   ├── PackingSessionService.java
│   │   └── ShipmentService.java
│   ├── command/                     # Commands
│   │   ├── StartPackingSessionCommand.java
│   │   ├── ScanItemCommand.java
│   │   ├── SelectCartonCommand.java
│   │   ├── PackItemCommand.java
│   │   ├── WeighPackageCommand.java
│   │   └── PerformQualityCheckCommand.java
│   └── query/                       # Queries
└── adapter/                          # External adapters
    ├── rest/                        # REST controllers
    │   └── PackingSessionController.java
    ├── persistence/                 # MongoDB repositories
    ├── binpacking/                  # 3D packing algorithms
    │   ├── FirstFitDecreasing.java
    │   └── BestFitOptimizer.java
    └── events/                      # Event publishers/consumers
```

### Design Patterns & Principles
- **Hexagonal Architecture** - Clean separation of domain and infrastructure
- **Domain-Driven Design** - Rich domain model with packing workflows
- **Strategy Pattern** - Pluggable bin packing algorithms
- **3D Bin Packing** - NP-hard volumetric optimization
- **First-Fit Decreasing** - Greedy bin packing heuristic
- **Event-Driven Architecture** - Real-time packing event publishing
- **State Machine Pattern** - Multi-stage packing workflow
- **Repository Pattern** - Data access abstraction
- **SOLID Principles** - Maintainable and extensible code

## Technology Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.3.3** - Application framework
- **Maven** - Build and dependency management

### Data & Persistence
- **MongoDB** - Document database for session storage
- **Spring Data MongoDB** - Data access layer

### Messaging & Events
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **CloudEvents 2.5.0** - Standardized event format

### API & Documentation
- **Spring Web MVC** - REST API framework
- **Bean Validation** - Input validation
- **OpenAPI/Swagger** - API documentation

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Micrometer Tracing** - Distributed tracing
- **Loki Logback Appender** - Log aggregation

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local development environment

## Standards Applied

### Architectural Standards
- ✅ Hexagonal Architecture (Ports and Adapters)
- ✅ Domain-Driven Design tactical patterns
- ✅ Event-Driven Architecture
- ✅ Microservices architecture
- ✅ RESTful API design
- ✅ Algorithm-driven optimization

### Code Quality Standards
- ✅ SOLID principles
- ✅ Clean Code practices
- ✅ Comprehensive unit and integration testing
- ✅ Domain-driven design patterns
- ✅ Immutable value objects
- ✅ Rich domain models with business logic

### Event & Integration Standards
- ✅ CloudEvents specification v1.0
- ✅ Event-driven workflow coordination
- ✅ At-least-once delivery semantics
- ✅ Event versioning strategy
- ✅ Idempotent event handling

### Observability Standards
- ✅ Structured logging (JSON)
- ✅ Distributed tracing
- ✅ Health check endpoints
- ✅ Prometheus metrics
- ✅ Correlation ID propagation

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/paklog/pack-ship-service.git
   cd pack-ship-service
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d mongodb kafka
   ```

3. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8084/actuator/health
   ```

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f pack-ship-service

# Stop all services
docker-compose down
```

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8084/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8084/v3/api-docs

### Key Endpoints

- `POST /packing-sessions` - Create new packing session
- `GET /packing-sessions/{sessionId}` - Get session details
- `POST /packing-sessions/{sessionId}/scan-item` - Scan item barcode
- `POST /packing-sessions/{sessionId}/select-carton` - Select packing carton
- `POST /packing-sessions/{sessionId}/pack-item` - Pack item into container
- `POST /packing-sessions/{sessionId}/weigh` - Weigh package
- `POST /packing-sessions/{sessionId}/quality-check` - Perform quality inspection
- `POST /packing-sessions/{sessionId}/seal` - Seal container
- `POST /packing-sessions/{sessionId}/create-shipment` - Generate shipping label
- `POST /packing-sessions/{sessionId}/complete` - Complete session
- `GET /packing-sessions/{sessionId}/recommended-carton` - Get carton recommendation

## 3D Bin Packing Features

### Bin Packing Algorithm

The service implements sophisticated 3D bin packing for optimal carton selection:

**Algorithm**: First-Fit Decreasing (FFD) with Best-Fit Refinement

```
1. Sort items by volume (largest first)
2. For each item:
   a. Find smallest bin that can fit item
   b. Apply 3D placement optimization
   c. Update bin utilization
3. If no bin fits, recommend larger carton
4. Calculate packing efficiency percentage
```

**Complexity**: O(n log n) for sorting, O(n × m) for placement

**Features**:
- **Volume calculation**: L × W × H for items and cartons
- **Orientation optimization**: Test all 6 possible orientations
- **Fragility handling**: Lighter/fragile items on top
- **Cushioning requirements**: Space allocation for packing materials
- **Utilization scoring**: Maximize carton fill percentage

### Carton Selection Strategy

Intelligent carton recommendation based on:
- **Total item volume**: Sum of all item dimensions
- **Fragility index**: Special handling requirements
- **Cushioning needs**: Extra space for protection
- **Irregular shapes**: Non-standard item handling
- **Weight distribution**: Balance within container
- **Cost optimization**: Minimize dimensional weight charges

### Packing Material Calculation

Automatic calculation of required materials:
- **Bubble wrap**: Based on fragile item count and size
- **Packing paper**: Void fill estimation
- **Tape**: Standard 6 inches per seal
- **Corner protectors**: For high-value items
- **Desiccant packs**: For moisture-sensitive items

## Packing Workflow

### Multi-Stage Process

```
SCANNING → CARTON_SELECTION → PACKING → WEIGHING → QUALITY_CHECK → LABELING → COMPLETED
```

### Quality Control Checkpoints

- **Item verification**: All expected items scanned
- **Damage inspection**: No damaged items packed
- **Packing quality**: Proper cushioning and void fill
- **Weight validation**: Within 5% of estimated weight
- **Label accuracy**: Correct address and carrier
- **Seal integrity**: Proper tape application

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration

Key configuration properties:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/pack_ship
  kafka:
    bootstrap-servers: localhost:9092

pack-ship:
  bin-packing:
    algorithm: first-fit-decreasing
    enable-orientation-optimization: true
  quality-control:
    weight-tolerance-percentage: 5.0
    mandatory-checkpoints:
      - ITEM_VERIFICATION
      - DAMAGE_INSPECTION
      - WEIGHT_VALIDATION
  cartons:
    available-sizes:
      - SMALL_BOX: 12x9x4
      - MEDIUM_BOX: 18x14x8
      - LARGE_BOX: 24x18x12
```

## Event Integration

### Published Events
- `com.paklog.wes.pack.session.started.v1`
- `com.paklog.wes.pack.item.scanned.v1`
- `com.paklog.wes.pack.item.packed.v1`
- `com.paklog.wes.pack.container.sealed.v1`
- `com.paklog.wes.pack.quality.check.completed.v1`
- `com.paklog.wes.pack.session.completed.v1`
- `com.paklog.wes.pack.shipment.created.v1`

### Consumed Events
- `com.paklog.wes.pick.session.completed.v1` - Initialize packing from completed pick
- `com.paklog.wes.task.assigned.v1` - Pack task assignment
- `com.paklog.cartonization.solution.calculated.v1` - Use pre-calculated packing plan

### Event Format
All events follow the CloudEvents specification v1.0 and are published asynchronously via Kafka.

## Packing Session Lifecycle

```
CREATED → SCANNING → READY_FOR_CARTON → PACKING → READY_TO_WEIGH → QC_PASSED → READY_TO_SHIP → COMPLETED
              ↓                                                           ↓
          CANCELLED                                                  QC_FAILED → PACKING (rework)
```

## Monitoring

- **Health**: http://localhost:8084/actuator/health
- **Metrics**: http://localhost:8084/actuator/metrics
- **Prometheus**: http://localhost:8084/actuator/prometheus
- **Info**: http://localhost:8084/actuator/info

### Key Metrics
- `packing.sessions.created.total` - Total sessions created
- `packing.sessions.completed.total` - Total sessions completed
- `packing.items.scanned.total` - Total items scanned
- `packing.carton.utilization.percentage` - Average carton fill rate
- `packing.quality.check.pass.rate` - QC pass percentage
- `packing.weight.discrepancy.rate` - Weight variance incidents
- `packing.session.duration.seconds` - Average packing time

## Contributing

1. Follow hexagonal architecture principles
2. Implement domain logic in domain layer
3. Optimize carton selection using bin packing algorithms
4. Maintain packing workflow state transitions
5. Enforce quality control checkpoints
6. Write comprehensive tests including algorithm tests
7. Document domain concepts using ubiquitous language
8. Follow existing code style and conventions

## License

Copyright © 2024 Paklog. All rights reserved.
