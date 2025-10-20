---
layout: default
title: Home
---

# Pack & Ship Service Documentation

Order packing and shipping preparation service with advanced 3D bin packing algorithms and multi-criteria carton selection.

## Overview

The Pack & Ship Service manages the complete packing and shipping workflow from carton selection through label generation. It implements sophisticated 3D bin packing algorithms for optimal carton utilization, multi-criteria carton selection based on volume, weight, cost, and protection requirements, and integrates with multiple carriers for label generation. The service uses Domain-Driven Design with event-driven architecture for efficient packing operations.

## Quick Links

### Getting Started
- [README](README.md) - Quick start guide and overview
- [Architecture Overview](architecture.md) - System architecture description

### Architecture & Design
- [Domain Model](DOMAIN-MODEL.md) - Complete domain model with class diagrams
- [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) - Process flows and interactions
- [OpenAPI Specification](openapi.yaml) - REST API documentation
- [AsyncAPI Specification](asyncapi.yaml) - Event documentation

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2** - Application framework
- **MongoDB** - Document database for shipment storage
- **Apache Kafka** - Event streaming platform
- **CloudEvents 2.5.0** - Event standard
- **Maven** - Build tool

## Key Features

- **3D Bin Packing Algorithm** - Optimal item placement in cartons
- **Multi-Criteria Carton Selection** - Volume, weight, cost, protection scoring
- **Multiple Carrier Integration** - FedEx, UPS, USPS, DHL, Amazon
- **Dimensional Weight Calculation** - Accurate shipping cost estimation
- **Carton Utilization Optimization** - Maximize fill rate and efficiency
- **Packing Visualization** - 3D packing layout generation
- **Label Generation** - Carrier-specific shipping labels
- **Customs Documentation** - International shipping support

## Domain Model

### Aggregates
- **Shipment** - Complete shipment lifecycle management

### Entities
- **Carton** - Individual carton with packed items
- **PackedItem** - Items placed in carton with position

### Value Objects
- **CartonType** - Carton specifications and constraints
- **Dimensions** - Length, width, height measurements
- **Position** - 3D coordinates (x, y, z)
- **PackItem** - Product details for packing
- **PackingResult** - Packing outcome with placement details
- **ShippingLabel** - Carrier label information
- **Address** - Shipping destination

### Shipment Lifecycle

```
CREATED -> PACKING -> PACKED -> LABELED -> SHIPPED -> IN_TRANSIT -> DELIVERED
                                      \-> CANCELLED
```

## Domain Events

### Published Events
- **ShipmentCreated** - New shipment created
- **CartonSelected** - Optimal carton selected
- **ItemPacked** - Item placed in carton
- **PackingCompleted** - All items packed
- **CartonSealed** - Carton sealed and ready
- **LabelGenerated** - Shipping label created
- **ShipmentShipped** - Shipment dispatched
- **TrackingUpdated** - Tracking status updated
- **ShipmentDelivered** - Shipment delivered
- **ShipmentCancelled** - Shipment cancelled

### Consumed Events
- **PickingCompleted** - Items ready for packing
- **OrderReadyToPack** - Order available for packing
- **CarrierCutoffApproaching** - Urgent packing needed

## Architecture Patterns

- **Hexagonal Architecture** - Ports and adapters for clean separation
- **Domain-Driven Design** - Rich domain model with business logic
- **Event-Driven Architecture** - Asynchronous integration via events
- **Strategy Pattern** - Multiple packing and carton selection strategies
- **Factory Pattern** - Carton and packing result creation

## API Endpoints

### Shipment Management
- `POST /shipments` - Create shipment
- `GET /shipments/{shipmentId}` - Get shipment details
- `PUT /shipments/{shipmentId}/pack` - Pack shipment
- `POST /shipments/{shipmentId}/labels` - Generate labels
- `POST /shipments/{shipmentId}/ship` - Mark as shipped
- `POST /shipments/{shipmentId}/cancel` - Cancel shipment
- `GET /shipments` - List shipments with filtering

### Carton Operations
- `POST /shipments/{shipmentId}/cartons` - Add carton
- `GET /shipments/{shipmentId}/cartons/{cartonId}` - Get carton details
- `POST /shipments/{shipmentId}/cartons/{cartonId}/seal` - Seal carton
- `GET /shipments/{shipmentId}/cartons/{cartonId}/visualization` - Get 3D packing view

### Carton Selection
- `POST /carton-selector/optimal` - Get optimal carton for items
- `POST /carton-selector/suggestions` - Get carton alternatives
- `POST /carton-selector/simulate` - Simulate packing

### Label Operations
- `POST /labels/generate` - Generate shipping label
- `GET /labels/{trackingNumber}` - Get label details
- `POST /labels/batch` - Batch label generation

## 3D Bin Packing Algorithm

### Algorithm Overview

The service implements a space-partitioning bin packing algorithm:

1. **Initialize** - Create initial space matching carton dimensions
2. **Item Selection** - Sort items by volume (largest first)
3. **Placement** - For each item:
   - Find best fitting space
   - Try all valid orientations
   - Place item and split remaining space
   - Check collision detection
4. **Optimization** - Continue until all items placed or no space remains

### Key Operations

#### Space Splitting
When an item is placed in a space, remaining volume is split into new spaces:
- Above the item
- To the right of the item
- Behind the item

#### Orientation Testing
Each item can be rotated to test multiple orientations:
- Original (L × W × H)
- Rotated 90° on X, Y, Z axes
- Rotated 180° on X, Y axes

#### Collision Detection
Verify placed items don't overlap using bounding box intersection tests.

### Algorithm Complexity
- Time: O(n × m × k) where n = items, m = spaces, k = orientations
- Space: O(m) for active spaces

## Carton Selection

### Multi-Criteria Scoring

The service evaluates cartons using weighted criteria:

1. **Volume Score (30%)** - Optimal fill rate
   - Penalize too small (items won't fit)
   - Penalize too large (wasted space)
   - Optimal: 80-90% utilization

2. **Weight Score (25%)** - Weight capacity
   - Ensure within carton max weight
   - Consider dimensional weight

3. **Cost Score (25%)** - Total shipping cost
   - Carton material cost
   - Shipping cost based on dim weight

4. **Protection Score (20%)** - Product safety
   - Fragile items need sturdy cartons
   - Hazmat compliance
   - Stackability requirements

### Carton Types

Standard carton types supported:
- **Small Box** - 8×6×4 inches, max 10 lbs
- **Medium Box** - 12×9×6 inches, max 20 lbs
- **Large Box** - 18×14×12 inches, max 40 lbs
- **Extra Large Box** - 24×18×18 inches, max 70 lbs
- **Pallet** - 48×40×48 inches, max 2000 lbs
- **Custom** - Configurable dimensions

## Dimensional Weight Calculation

Dimensional weight (DIM weight) is calculated per carrier rules:

```
DIM Weight = (Length × Width × Height) / DIM Factor
```

### DIM Factors by Carrier
- **FedEx/UPS**: 139 (domestic), 166 (international)
- **USPS**: 166
- **DHL**: 139

### Chargeable Weight
```
Chargeable Weight = MAX(Actual Weight, DIM Weight)
```

## Carrier Integration

### Supported Carriers
- **FedEx** - Express, Ground, Home Delivery
- **UPS** - Ground, 3-Day, 2-Day, Next Day
- **USPS** - Priority, Priority Express, First Class
- **DHL** - Express, Economy
- **Amazon** - Amazon Logistics

### Label Generation
- Carrier-specific label formats (ZPL, PDF, PNG)
- Tracking number assignment
- Customs forms for international
- Return labels
- Multi-package shipments

## Packing Strategies

### Standard Packing
Items packed in order by volume, largest first.

### Priority Packing
Fragile and high-value items placed first with optimal cushioning.

### Multi-Carton Packing
Automatically split items across multiple cartons when needed.

### Weight Distribution
Balance weight across cartons for handling and shipping efficiency.

## Integration Points

### Consumes Events From
- Pick Execution (picking completed)
- Order Management (order ready to pack)
- Inventory (item dimensions/weight)

### Publishes Events To
- Order Management (packing completed)
- Physical Tracking (carton movements)
- Shipment Transportation (ready to ship)
- Inventory (items packed/shipped)

## Performance Considerations

### Algorithm Optimization
- Item pre-sorting by volume for faster placement
- Space pruning to remove infeasible spaces
- Early termination on perfect fits
- Orientation caching for identical items

### Caching Strategy
- Carton type specifications cached
- Carrier rate tables cached (1 hour TTL)
- Dimensional weight factors cached
- Label templates cached

### Batch Processing
- Batch label generation for efficiency
- Bulk packing simulation
- Aggregate shipment metrics

## Business Rules

1. **Packing Rules**
   - Fragile items cannot be bottom layer
   - Hazmat items require specific cartons
   - Maximum carton weight enforced
   - Minimum fill rate: 60% (configurable)

2. **Carton Selection Rules**
   - Try smallest carton that fits first
   - Prefer standard sizes over custom
   - Consider protection requirements
   - Optimize for shipping cost

3. **Label Generation Rules**
   - Require valid destination address
   - Enforce carrier service level requirements
   - Validate customs info for international
   - Track label expiration

4. **Shipping Rules**
   - Carton must be sealed before shipping
   - Label must be generated
   - Weight verification required
   - Carrier cutoff times enforced

## Metrics and KPIs

### Packing Metrics
- Average carton utilization percentage
- Packing time per shipment
- Multi-carton percentage
- Carton selection accuracy

### Shipping Metrics
- On-time shipping percentage
- Average shipping cost per order
- Label generation success rate
- Dimensional weight vs. actual weight ratio

## Getting Started

1. Review the [README](README.md) for quick start instructions
2. Understand the [Architecture](architecture.md) and design patterns
3. Explore the [Domain Model](DOMAIN-MODEL.md) to understand business concepts
4. Study the [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) for process flows
5. Reference the [OpenAPI](openapi.yaml) and [AsyncAPI](asyncapi.yaml) specifications

## Configuration

Key configuration properties:
- `packing.min-fill-rate` - Minimum carton utilization (default: 0.60)
- `packing.max-orientations` - Max rotation attempts (default: 6)
- `packing.allow-multi-carton` - Enable carton splitting (default: true)
- `carton.selection.volume-weight` - Volume score weight (default: 0.30)
- `carton.selection.weight-weight` - Weight score weight (default: 0.25)
- `carton.selection.cost-weight` - Cost score weight (default: 0.25)
- `carton.selection.protection-weight` - Protection score weight (default: 0.20)
- `carrier.rate-cache-ttl` - Rate cache TTL (default: 1h)
- `carrier.label-format` - Default label format (ZPL, PDF, PNG)

## Contributing

For contribution guidelines, please refer to the main README in the project root.

## Support

- **GitHub Issues**: Report bugs or request features
- **Documentation**: Browse the guides in the navigation menu
- **Service Owner**: Fulfillment Team
- **Slack**: #fulfillment-pack-ship
