# Pack & Ship Service - Implementation Plan

**Service**: Pack & Ship Service (WES)
**Port**: 8083
**Database**: MongoDB (pack_ship)
**Pattern**: Domain-Driven Design + Hexagonal Architecture

---

## Overview

The Pack & Ship Service manages the packing and shipping operations after items are picked. It handles container selection, packing validation, shipping label generation, and carrier integration.

### Key Responsibilities
- Packing session management (single order and batch packing)
- Container optimization (box/pallet selection)
- Packing validation and quality checks
- Shipping label generation
- Carrier integration (UPS, FedEx, USPS)
- Shipment tracking and manifesting
- Packing accuracy verification

---

## Domain Model

### Aggregates (2)

#### 1. PackingSession
- **Purpose**: Manages the complete packing workflow
- **Lifecycle**: CREATED → IN_PROGRESS → QUALITY_CHECK → COMPLETED / CANCELLED
- **Key Operations**:
  - Create packing session from pick session
  - Add item to container
  - Validate packing accuracy
  - Complete packing
  - Quality verification
  - Cancel session

#### 2. Shipment
- **Purpose**: Manages shipping and carrier operations
- **Lifecycle**: CREATED → LABELED → MANIFESTED → SHIPPED → IN_TRANSIT → DELIVERED
- **Key Operations**:
  - Create shipment from packing session
  - Generate shipping label
  - Add to carrier manifest
  - Mark as shipped
  - Update tracking status
  - Handle exceptions

### Entities (3)

#### 1. PackingInstruction
- Item to be packed
- Expected quantity
- Packing status (PENDING, PACKED, MISSING, DAMAGED)
- Container assignment
- Packing verification

#### 2. Container
- Container ID
- Type (SMALL_BOX, MEDIUM_BOX, LARGE_BOX, PALLET, TOTE)
- Dimensions and weight capacity
- Items packed
- Utilization percentage
- Container status (OPEN, SEALED, LABELED)

#### 3. ShippingLabel
- Tracking number
- Carrier barcode
- Label format (PDF, ZPL, PNG)
- Label data (base64)
- Generated timestamp
- Carrier reference

### Value Objects (8)

#### 1. PackingStatus
- CREATED
- IN_PROGRESS
- QUALITY_CHECK
- COMPLETED
- CANCELLED
- FAILED

#### 2. ContainerType
- SMALL_BOX (12x9x6)
- MEDIUM_BOX (18x14x12)
- LARGE_BOX (24x18x18)
- EXTRA_LARGE_BOX (36x24x24)
- PALLET (48x40x48)
- TOTE (23x15x12)
- CUSTOM

#### 3. ShippingMethod
- GROUND
- TWO_DAY
- NEXT_DAY
- SAME_DAY
- FREIGHT
- INTERNATIONAL

#### 4. CarrierType
- UPS
- FEDEX
- USPS
- DHL
- AMAZON_LOGISTICS
- CUSTOM

#### 5. Address
- Street lines
- City, state, zip
- Country
- Validation status

#### 6. Weight
- Value and unit (LB, KG)
- Validation and conversions

#### 7. Dimensions
- Length, width, height
- Unit (IN, CM)
- Volume calculation

#### 8. TrackingStatus
- CREATED
- LABELED
- MANIFESTED
- PICKED_UP
- IN_TRANSIT
- OUT_FOR_DELIVERY
- DELIVERED
- EXCEPTION
- RETURNED

### Domain Events (7)

1. **PackingSessionStartedEvent**
   - sessionId, pickSessionId, orderId, workerId

2. **ItemPackedEvent**
   - sessionId, itemSku, containerId, quantity

3. **ContainerSealedEvent**
   - sessionId, containerId, itemCount, weight

4. **PackingSessionCompletedEvent**
   - sessionId, orderId, containerCount, totalWeight, accuracy

5. **ShipmentCreatedEvent**
   - shipmentId, orderId, carrier, shippingMethod

6. **ShippingLabelGeneratedEvent**
   - shipmentId, trackingNumber, carrier

7. **ShipmentDispatchedEvent**
   - shipmentId, trackingNumber, dispatchTime

---

## Layer Structure

### 1. Domain Layer
**Package**: `com.paklog.wes.pack.domain`

```
domain/
├── aggregate/
│   ├── PackingSession.java
│   └── Shipment.java
├── entity/
│   ├── PackingInstruction.java
│   ├── Container.java
│   └── ShippingLabel.java
├── valueobject/
│   ├── PackingStatus.java
│   ├── ContainerType.java
│   ├── ShippingMethod.java
│   ├── CarrierType.java
│   ├── Address.java
│   ├── Weight.java
│   ├── Dimensions.java
│   └── TrackingStatus.java
├── event/
│   ├── PackingSessionStartedEvent.java
│   ├── ItemPackedEvent.java
│   ├── ContainerSealedEvent.java
│   ├── PackingSessionCompletedEvent.java
│   ├── ShipmentCreatedEvent.java
│   ├── ShippingLabelGeneratedEvent.java
│   └── ShipmentDispatchedEvent.java
├── repository/
│   ├── PackingSessionRepository.java
│   └── ShipmentRepository.java
└── service/
    ├── ContainerOptimizationService.java
    └── ShippingLabelService.java
```

### 2. Application Layer
**Package**: `com.paklog.wes.pack.application`

```
application/
├── command/
│   ├── StartPackingSessionCommand.java
│   ├── PackItemCommand.java
│   ├── SealContainerCommand.java
│   ├── CreateShipmentCommand.java
│   └── GenerateLabelCommand.java
└── service/
    ├── PackingSessionService.java
    └── ShipmentService.java
```

### 3. Adapter Layer (REST API)
**Package**: `com.paklog.wes.pack.adapter.rest`

```
adapter/rest/
├── controller/
│   ├── PackingSessionController.java      // /api/v1/packing
│   ├── ShipmentController.java            // /api/v1/shipments
│   └── MobilePackController.java          // /api/v1/mobile/pack
└── dto/
    ├── StartPackingRequest.java
    ├── PackingSessionResponse.java
    ├── PackItemRequest.java
    ├── SealContainerRequest.java
    ├── CreateShipmentRequest.java
    ├── ShipmentResponse.java
    └── LabelResponse.java
```

### 4. Infrastructure Layer
**Package**: `com.paklog.wes.pack.infrastructure`

```
infrastructure/
├── config/
│   ├── MongoConfig.java
│   ├── KafkaConfig.java
│   └── OpenAPIConfig.java
└── adapter/
    ├── CarrierAdapter.java (interface)
    ├── UPSAdapter.java
    ├── FedExAdapter.java
    └── MockCarrierAdapter.java
```

---

## API Endpoints

### Packing Session API (`/api/v1/packing`)

1. **POST /sessions**
   - Start new packing session
   - Input: pickSessionId, orderId, workerId, warehouseId
   - Output: PackingSessionResponse

2. **GET /sessions/{id}**
   - Get packing session details
   - Output: PackingSessionResponse

3. **POST /sessions/{id}/pack**
   - Pack item into container
   - Input: instructionId, containerId, quantity
   - Output: Updated session

4. **POST /sessions/{id}/seal**
   - Seal container
   - Input: containerId, weight
   - Output: Container details

5. **POST /sessions/{id}/complete**
   - Complete packing session
   - Output: Completed session

6. **POST /sessions/{id}/cancel**
   - Cancel packing session
   - Input: reason
   - Output: Cancelled session

7. **GET /sessions/{id}/containers**
   - List all containers in session
   - Output: List of containers

8. **GET /sessions/active**
   - Get active packing sessions
   - Query params: warehouseId, workerId

### Shipment API (`/api/v1/shipments`)

1. **POST /**
   - Create shipment from packing session
   - Input: packingSessionId, shippingAddress, carrier, method
   - Output: ShipmentResponse

2. **GET /{id}**
   - Get shipment details
   - Output: ShipmentResponse

3. **POST /{id}/label**
   - Generate shipping label
   - Output: LabelResponse (with PDF/ZPL data)

4. **POST /{id}/manifest**
   - Add to carrier manifest
   - Output: Manifest confirmation

5. **POST /{id}/dispatch**
   - Mark shipment as dispatched
   - Output: Updated shipment

6. **GET /{id}/tracking**
   - Get tracking information
   - Output: Tracking events

7. **GET /by-order/{orderId}**
   - Get shipments for order
   - Output: List of shipments

### Mobile Packing API (`/api/v1/mobile/pack`)

1. **GET /my-session**
   - Get worker's active packing session
   - Header: X-Worker-Id

2. **GET /current-instruction**
   - Get next item to pack
   - Header: X-Worker-Id

3. **POST /pack**
   - Pack item (simplified)
   - Input: instructionId, containerId, quantity

4. **POST /seal**
   - Seal current container
   - Input: containerId, weight

5. **GET /containers**
   - List session containers
   - Header: X-Worker-Id

---

## Domain Services

### 1. ContainerOptimizationService
**Purpose**: Optimize container selection for packing

**Key Methods**:
- `recommendContainer(List<PackingInstruction> items)`: Recommend best container
- `calculateUtilization(Container container)`: Calculate space utilization
- `suggestContainerSplit(List<PackingInstruction> items)`: Suggest multiple containers
- `validateContainerCapacity(Container container, PackingInstruction item)`: Check if item fits

**Algorithm**: Bin packing with weight and dimension constraints

### 2. ShippingLabelService
**Purpose**: Generate shipping labels for carriers

**Key Methods**:
- `generateLabel(Shipment shipment, CarrierType carrier)`: Generate label
- `getTrackingNumber(CarrierType carrier)`: Allocate tracking number
- `formatLabelData(Shipment shipment, String format)`: Format label (PDF, ZPL, PNG)
- `validateShippingAddress(Address address)`: Validate address

**Integration**: Mock carrier APIs (extensible for real integrations)

---

## MongoDB Collections

### 1. packing_sessions
```javascript
{
  _id: "PACK-ABC123",
  pickSessionId: "SESSION-XYZ",
  orderId: "ORDER-001",
  workerId: "WORKER-123",
  warehouseId: "WH-001",
  status: "IN_PROGRESS",
  packingInstructions: [
    {
      instructionId: "INST-001",
      itemSku: "SKU-12345",
      expectedQuantity: 10,
      packedQuantity: 10,
      containerId: "CONT-001",
      status: "PACKED"
    }
  ],
  containers: [
    {
      containerId: "CONT-001",
      type: "MEDIUM_BOX",
      dimensions: {length: 18, width: 14, height: 12, unit: "IN"},
      weight: {value: 15.5, unit: "LB"},
      items: ["INST-001"],
      status: "SEALED"
    }
  ],
  createdAt: ISODate("2025-10-18T10:00:00Z"),
  completedAt: ISODate("2025-10-18T10:30:00Z")
}
```

### 2. shipments
```javascript
{
  _id: "SHIP-123",
  packingSessionId: "PACK-ABC123",
  orderId: "ORDER-001",
  carrier: "UPS",
  shippingMethod: "GROUND",
  trackingNumber: "1Z999AA10123456784",
  status: "IN_TRANSIT",
  shippingAddress: {
    street: "123 Main St",
    city: "New York",
    state: "NY",
    zip: "10001",
    country: "US"
  },
  weight: {value: 15.5, unit: "LB"},
  dimensions: {length: 18, width: 14, height: 12, unit: "IN"},
  shippingLabel: {
    trackingNumber: "1Z999AA10123456784",
    format: "PDF",
    labelData: "base64...",
    generatedAt: ISODate("2025-10-18T10:30:00Z")
  },
  createdAt: ISODate("2025-10-18T10:30:00Z"),
  shippedAt: ISODate("2025-10-18T11:00:00Z")
}
```

---

## MongoDB Indexes

1. **idx_pick_session_id** - Lookup by pick session
2. **idx_order_id** - Lookup by order
3. **idx_worker_status** - Active sessions by worker
4. **idx_warehouse_status** - Warehouse queries
5. **idx_status** - Status filtering
6. **idx_tracking_number** - Shipment tracking
7. **idx_created_at** - Time-based queries

---

## Configuration (application.yml)

```yaml
server:
  port: 8083

spring:
  application:
    name: pack-ship-service
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/pack_ship}
      database: pack_ship

# Pack & Ship Configuration
pack:
  container:
    auto-select: true
    max-weight-lb: 70
  shipping:
    default-carrier: UPS
    label-format: PDF

paklog:
  kafka:
    topics:
      pick-events: wes-pick-events
      pack-events: wes-pack-events
      ship-events: wes-ship-events
```

---

## Integration Points

### Consumes From
- **Pick Execution Service**
  - Event: `PickSessionCompletedEvent` (wes-pick-events topic)
  - Trigger: Start packing session when picks complete

### Publishes To
- **Pack Events Topic** (wes-pack-events)
  - `PackingSessionStartedEvent`
  - `ItemPackedEvent`
  - `ContainerSealedEvent`
  - `PackingSessionCompletedEvent`

- **Ship Events Topic** (wes-ship-events)
  - `ShipmentCreatedEvent`
  - `ShippingLabelGeneratedEvent`
  - `ShipmentDispatchedEvent`

---

## Testing Strategy

### Unit Tests (Target: 15+ tests)
- PackingSessionTest (lifecycle, item packing, container sealing)
- ShipmentTest (label generation, tracking updates)
- ContainerOptimizationServiceTest (bin packing, utilization)
- ShippingLabelServiceTest (label generation, validation)

---

## Implementation Phases

### Phase 1: Domain Layer
1. Value objects (8 files)
2. Entities (3 files)
3. Domain events (7 files)
4. Aggregate roots (2 files)
5. Repositories (2 files)
6. Domain services (2 files)

### Phase 2: Application Layer
1. Commands (5 files)
2. Application services (2 files)

### Phase 3: REST API Layer
1. Controllers (3 files)
2. DTOs (7 files)

### Phase 4: Infrastructure & Testing
1. MongoDB configuration and indexes
2. Application configuration
3. Unit tests (1 file, 15+ tests)

### Phase 5: Build & Verification
1. Maven build
2. Test execution
3. Documentation

---

## Success Criteria

- ✅ All domain model implemented with DDD patterns
- ✅ Container optimization algorithm working
- ✅ Shipping label generation (mock)
- ✅ 15+ REST API endpoints
- ✅ MongoDB integration with indexes
- ✅ 15+ unit tests (100% pass rate)
- ✅ Build succeeds
- ✅ Integration with Pick Execution Service

**Target Completion**: 25-30 production files, 15+ tests, 15+ API endpoints

---

**Status**: Ready to implement
**Next Step**: Begin Phase 1 - Domain Layer implementation
