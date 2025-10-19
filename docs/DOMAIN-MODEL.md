# Pack Ship Service - Domain Model

## Overview

The Pack Ship Service domain model implements sophisticated packing and shipping operations using Domain-Driven Design. The model centers around the Shipment Aggregate with advanced 3D bin packing algorithms and multi-criteria carton selection.

## Class Diagram

```mermaid
classDiagram
    class Shipment {
        +String shipmentId
        +String orderId
        +String customerId
        +ShipmentStatus status
        +ShippingMethod method
        +Carrier carrier
        +Address destination
        +List~Carton~ cartons
        +ShipmentMetrics metrics
        +LocalDateTime createdAt
        +LocalDateTime shippedAt
        +createShipment()
        +addCarton(carton)
        +generateLabels()
        +markShipped()
        +updateTracking(status)
        +calculateCost()
    }

    class ShipmentStatus {
        <<enumeration>>
        CREATED
        PACKING
        PACKED
        LABELED
        SHIPPED
        IN_TRANSIT
        DELIVERED
        CANCELLED
    }

    class Carton {
        +String cartonId
        +CartonType type
        +List~PackedItem~ packedItems
        +Dimensions dimensions
        +double actualWeight
        +double dimWeight
        +double chargeableWeight
        +PackingResult packingResult
        +String trackingNumber
        +ShippingLabel label
        +addItem(item, placement)
        +calculateWeights()
        +generateLabel()
        +seal()
    }

    class CartonType {
        +String typeId
        +String name
        +Dimensions innerDimensions
        +Dimensions outerDimensions
        +double maxWeight
        +double tareWeight
        +double unitCost
        +MaterialType material
        +ProtectionLevel protection
        +boolean isActive
    }

    class CartonSelector {
        +Carton selectOptimalCarton(items)
        +List~CartonSuggestion~ suggestAlternatives(items)
        +PackingResult tryPack3D(items, cartonType)
        +List~Carton~ splitIntoMultipleCartons(items)
        +double calculateScore(items, cartonType)
        -double calculateVolumeScore()
        -double calculateWeightScore()
        -double calculateCostScore()
        -double calculateProtectionScore()
    }

    class BinPacker3D {
        +PackingResult pack(items, container)
        +List~PlacedItem~ findPlacements(items, spaces)
        +boolean canFit(item, space, orientation)
        +List~Space~ splitSpace(space, placedItem)
        +boolean checkCollision(item1, item2)
        +List~Orientation~ getAllOrientations(item)
    }

    class PackingResult {
        +boolean success
        +List~PlacedItem~ placements
        +double utilization
        +double totalVolume
        +double usedVolume
        +String message
        +PackingVisualization visualization
    }

    class PlacedItem {
        +PackItem item
        +Position position
        +Orientation orientation
        +BoundingBox boundingBox
        +boolean isRotated()
        +boolean overlaps(other)
    }

    class PackItem {
        +String itemId
        +String sku
        +String productName
        +int quantity
        +Dimensions dimensions
        +double weight
        +boolean fragile
        +boolean hazmat
        +StackabilityType stackability
        +List~String~ handlingInstructions
    }

    class Space {
        +Position corner
        +Dimensions dimensions
        +double volume
        +boolean canAccommodate(item, orientation)
        +List~Space~ split(placedItem)
    }

    class Position {
        +double x
        +double y
        +double z
        +Position translate(dx, dy, dz)
        +double distanceTo(other)
    }

    class Dimensions {
        +double length
        +double width
        +double height
        +double getVolume()
        +Dimensions rotate(orientation)
        +boolean fitsInside(other)
    }

    class Orientation {
        <<enumeration>>
        ORIGINAL
        ROTATED_90_X
        ROTATED_90_Y
        ROTATED_90_Z
        ROTATED_180_X
        ROTATED_180_Y
    }

    class CartonSuggestion {
        +CartonType cartonType
        +CartonScore score
        +String recommendation
        +List~String~ pros
        +List~String~ cons
        +PackingSimulation simulation
    }

    class CartonScore {
        +double totalScore
        +double volumeScore
        +double weightScore
        +double costScore
        +double protectionScore
        +double fillRate
        +double efficiency
    }

    class ShippingLabel {
        +String trackingNumber
        +Carrier carrier
        +String serviceLevel
        +byte[] labelImage
        +String labelFormat
        +Map~String String~ customsInfo
        +LocalDateTime createdAt
        +LocalDateTime expiresAt
    }

    class Carrier {
        <<enumeration>>
        FEDEX
        UPS
        USPS
        DHL
        AMAZON
    }

    class ShippingMethod {
        +String methodId
        +String name
        +Carrier carrier
        +String serviceCode
        +int transitDays
        +double baseCost
        +boolean isExpedited
        +boolean requiresSignature
    }

    class RateCalculator {
        +ShippingRate calculateRate(shipment)
        +List~RateOption~ getRateOptions(shipment)
        +double calculateDimWeight(dimensions)
        +double getZoneSurcharge(origin, destination)
        +double getFuelSurcharge(carrier)
    }

    class PackingStrategy {
        <<interface>>
        +List~Carton~ pack(items)
        +PackingResult simulate(items, carton)
    }

    class FFDStrategy {
        +List~Carton~ pack(items)
        +List~PackItem~ sortByVolume(items)
    }

    class BestFitStrategy {
        +List~Carton~ pack(items)
        +Carton findBestFit(item, cartons)
    }

    class LayerStrategy {
        +List~Carton~ pack(items)
        +List~Layer~ createLayers(items)
    }

    Shipment "1" *-- "1..*" Carton : contains
    Shipment "1" --> "1" ShipmentStatus : has
    Shipment "1" --> "1" Carrier : ships via
    Shipment "1" --> "1" ShippingMethod : uses
    Carton "1" *-- "0..*" PackedItem : contains
    Carton "1" --> "1" CartonType : is type
    Carton "1" --> "0..1" ShippingLabel : has
    Carton "1" --> "1" PackingResult : has
    PackingResult "1" *-- "0..*" PlacedItem : contains
    PlacedItem "1" --> "1" PackItem : references
    PlacedItem "1" --> "1" Position : at
    PlacedItem "1" --> "1" Orientation : with
    CartonSelector --> BinPacker3D : uses
    CartonSelector ..> CartonSuggestion : suggests
    BinPacker3D ..> PackingResult : produces
    BinPacker3D --> Space : manages
    PackingStrategy <|-- FFDStrategy : implements
    PackingStrategy <|-- BestFitStrategy : implements
    PackingStrategy <|-- LayerStrategy : implements
    RateCalculator ..> Shipment : calculates for
```

## Entity Relationships

```mermaid
erDiagram
    SHIPMENT ||--o{ CARTON : contains
    CARTON ||--o{ PACKED_ITEM : contains
    CARTON }o--|| CARTON_TYPE : is_type
    SHIPMENT }o--|| ORDER : fulfills
    SHIPMENT }o--|| CARRIER : ships_via
    CARTON ||--o| SHIPPING_LABEL : has
    PACKED_ITEM }o--|| PRODUCT : is
    CARTON_TYPE ||--o{ CARTON : used_by
    SHIPMENT }o--|| CUSTOMER : ships_to

    SHIPMENT {
        string shipment_id PK
        string order_id FK
        string customer_id FK
        string status
        string carrier
        string tracking_number
        timestamp created_at
        timestamp shipped_at
        json destination_address
    }

    CARTON {
        string carton_id PK
        string shipment_id FK
        string carton_type_id FK
        double actual_weight
        double dim_weight
        string tracking_number
        json packing_result
        timestamp packed_at
    }

    PACKED_ITEM {
        string item_id PK
        string carton_id FK
        string product_id FK
        int quantity
        json position
        json orientation
        json dimensions
    }

    CARTON_TYPE {
        string type_id PK
        string name
        double length
        double width
        double height
        double max_weight
        double unit_cost
        string material
    }

    SHIPPING_LABEL {
        string label_id PK
        string carton_id FK
        string tracking_number
        string carrier
        blob label_image
        timestamp created_at
    }
```

## Value Objects

### Dimensions
```java
public class Dimensions {
    private final double length;
    private final double width;
    private final double height;

    public double getVolume() {
        return length * width * height;
    }

    public double getDimensionalWeight(int divisor) {
        return getVolume() / divisor;
    }

    public Dimensions rotate(Orientation orientation) {
        // Return rotated dimensions
    }
}
```

### Position
```java
public class Position {
    private final double x;
    private final double y;
    private final double z;

    public boolean isWithin(BoundingBox box) {
        return x >= box.minX && x <= box.maxX &&
               y >= box.minY && y <= box.maxY &&
               z >= box.minZ && z <= box.maxZ;
    }
}
```

### BoundingBox
```java
public class BoundingBox {
    private final Position minCorner;
    private final Position maxCorner;

    public boolean intersects(BoundingBox other) {
        return !(maxCorner.x < other.minCorner.x ||
                minCorner.x > other.maxCorner.x ||
                maxCorner.y < other.minCorner.y ||
                minCorner.y > other.maxCorner.y ||
                maxCorner.z < other.minCorner.z ||
                minCorner.z > other.maxCorner.z);
    }
}
```

### Address
```java
public class Address {
    private final String street;
    private final String city;
    private final String state;
    private final String zipCode;
    private final String country;
    private final boolean residential;
}
```

## Domain Events

```mermaid
classDiagram
    class ShipmentEvent {
        <<abstract>>
        +String shipmentId
        +String orderId
        +Instant timestamp
        +String userId
    }

    class ShipmentCreated {
        +List~String~ itemIds
        +Address destination
        +ShippingMethod method
    }

    class CartonPacked {
        +String cartonId
        +CartonType type
        +List~PackedItem~ items
        +double fillRate
    }

    class CartonSelected {
        +CartonType selected
        +List~CartonSuggestion~ alternatives
        +CartonScore score
    }

    class PackingOptimized {
        +int originalCartons
        +int optimizedCartons
        +double costSavings
    }

    class LabelGenerated {
        +String cartonId
        +String trackingNumber
        +Carrier carrier
        +double shippingCost
    }

    class ShipmentShipped {
        +List~String~ trackingNumbers
        +Carrier carrier
        +LocalDateTime shipTime
    }

    class ShipmentDelivered {
        +LocalDateTime deliveryTime
        +String signature
        +DeliveryStatus status
    }

    class PackingFailed {
        +String reason
        +List~String~ problemItems
        +String suggestedAction
    }

    ShipmentEvent <|-- ShipmentCreated
    ShipmentEvent <|-- CartonPacked
    ShipmentEvent <|-- CartonSelected
    ShipmentEvent <|-- PackingOptimized
    ShipmentEvent <|-- LabelGenerated
    ShipmentEvent <|-- ShipmentShipped
    ShipmentEvent <|-- ShipmentDelivered
    ShipmentEvent <|-- PackingFailed
```

## Domain Services

### CartonSelector
Intelligent carton selection with 3D validation:
- `selectOptimalCarton()` - Find best carton for items
- `suggestAlternatives()` - Provide top 5 options
- `tryPack3D()` - Validate packing with 3D algorithm
- `splitIntoMultipleCartons()` - Handle oversized orders
- `calculateScore()` - Multi-criteria scoring

### BinPacker3D
3D bin packing implementation:
- `pack()` - Execute FFD packing algorithm
- `findPlacements()` - Find valid positions for items
- `canFit()` - Check if item fits in space
- `splitSpace()` - Manage remaining spaces
- `checkCollision()` - Validate no overlaps
- `getAllOrientations()` - Generate 6 rotations

### RateCalculator
Shipping rate calculation:
- `calculateRate()` - Get shipping cost
- `getRateOptions()` - Compare carrier rates
- `calculateDimWeight()` - Dimensional weight
- `getZoneSurcharge()` - Distance-based pricing
- `getFuelSurcharge()` - Current fuel adjustments

### LabelService
Shipping label generation:
- `generateLabel()` - Create carrier label
- `validateAddress()` - Address verification
- `getTracking()` - Tracking number assignment
- `printLabel()` - Format for printing
- `voidLabel()` - Cancel unused labels

## Algorithms

### 3D Bin Packing Algorithm (FFD)
```
1. Sort items by volume (largest first)
2. Initialize empty spaces list with container
3. For each item:
   a. For each orientation (6 total):
      - Find first space that fits
      - If found, place item
      - Split remaining space into 3 sub-spaces
      - Add sub-spaces to spaces list
      - Break to next item
   b. If no fit found, item doesn't fit
4. Calculate utilization metrics
5. Return packing result
```

### Carton Selection Scoring
```
1. For each available carton type:
   a. Try 3D packing simulation
   b. If items fit:
      - Calculate volume score (40%)
      - Calculate weight score (30%)
      - Calculate cost score (20%)
      - Calculate protection score (10%)
      - Total score = weighted sum
2. Sort cartons by total score
3. Return highest scoring carton
```

### Multi-Carton Splitting
```
1. Try single carton first
2. If doesn't fit:
   a. Sort items by priority/fragility
   b. Create first carton with priority items
   c. Fill remaining space optimally
   d. Repeat for remaining items
3. Optimize carton combinations
4. Return list of packed cartons
```

## Business Rules

1. **Carton Selection Rules**
   - Minimum 40% fill rate required
   - Maximum 85% weight capacity
   - Fragile items need protective cartons
   - Hazmat requires special packaging

2. **Packing Rules**
   - Heavy items on bottom
   - Fragile items on top
   - No mixing of incompatible items
   - Respect stacking limits

3. **Shipping Rules**
   - Labels expire after 24 hours
   - International requires customs docs
   - Signature required for high-value
   - Insurance for items >$100

4. **Cost Optimization**
   - Use dimensional weight divisor 166
   - Consider zone skipping
   - Batch similar destinations
   - Rate shop across carriers

## Performance Considerations

- 3D packing uses spatial indexing for O(log n) space searches
- Carton types cached in memory
- Rate calculations cached for 1 hour
- Async label generation for large batches
- MongoDB indexes on shipmentId, orderId, status, carrier