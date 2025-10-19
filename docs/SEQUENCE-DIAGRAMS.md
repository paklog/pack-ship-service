# Pack Ship Service - Sequence Diagrams

## 1. Carton Selection and Packing Flow

### Optimal Carton Selection with 3D Validation

```mermaid
sequenceDiagram
    autonumber
    participant PackStation
    participant PackController
    participant CartonSelector
    participant BinPacker3D
    participant InventoryService
    participant ShipmentRepository

    PackStation->>PackController: POST /pack/select-carton
    Note over PackStation: Order items to pack

    PackController->>CartonSelector: selectOptimalCarton(items)

    CartonSelector->>InventoryService: getItemDimensions(items)
    InventoryService-->>CartonSelector: List<ItemDetails>

    CartonSelector->>CartonSelector: getAvailableCartons()
    CartonSelector->>CartonSelector: filterByWeightCapacity()

    loop For each carton type
        CartonSelector->>BinPacker3D: tryPack3D(items, cartonType)

        BinPacker3D->>BinPacker3D: sortItemsByVolume()
        Note over BinPacker3D: Largest first (FFD)

        BinPacker3D->>BinPacker3D: initializeSpaces()

        loop For each item
            loop For each orientation (6)
                BinPacker3D->>BinPacker3D: findFittingSpace()

                alt Space found
                    BinPacker3D->>BinPacker3D: placeItem()
                    BinPacker3D->>BinPacker3D: splitRemainingSpace()
                    break Item placed
                end
            end
        end

        BinPacker3D->>BinPacker3D: calculateUtilization()
        BinPacker3D-->>CartonSelector: PackingResult

        alt Packing successful
            CartonSelector->>CartonSelector: calculateScore()
            Note over CartonSelector: Volume 40%, Weight 30%, Cost 20%, Protection 10%
        end
    end

    CartonSelector->>CartonSelector: rankByScore()
    CartonSelector->>CartonSelector: selectBest()

    CartonSelector-->>PackController: SelectedCarton + Alternatives
    PackController-->>PackStation: 200 OK (carton recommendation)
```

### Multi-Carton Order Split

```mermaid
sequenceDiagram
    autonumber
    participant PackStation
    participant PackController
    participant CartonSelector
    participant SplitStrategy
    participant BinPacker3D
    participant ShipmentService

    PackStation->>PackController: POST /pack/split-order
    Note over PackStation: Large order

    PackController->>CartonSelector: splitIntoMultipleCartons(items)

    CartonSelector->>BinPacker3D: tryPack3D(allItems, largestCarton)
    BinPacker3D-->>CartonSelector: PackingFailed

    CartonSelector->>SplitStrategy: determineStrategy(items)

    alt Volume-based split
        SplitStrategy->>SplitStrategy: sortByVolume()
        SplitStrategy->>SplitStrategy: createVolumeGroups()
    else Weight-based split
        SplitStrategy->>SplitStrategy: sortByWeight()
        SplitStrategy->>SplitStrategy: createWeightGroups()
    else Priority-based split
        SplitStrategy->>SplitStrategy: sortByPriority()
        SplitStrategy->>SplitStrategy: packPriorityFirst()
    end

    SplitStrategy-->>CartonSelector: ItemGroups

    loop For each item group
        CartonSelector->>CartonSelector: selectOptimalCarton(group)

        CartonSelector->>BinPacker3D: pack(group, carton)
        BinPacker3D-->>CartonSelector: PackingResult

        CartonSelector->>CartonSelector: createCarton()
        CartonSelector->>CartonSelector: addToShipment()
    end

    CartonSelector->>ShipmentService: createMultiPieceShipment(cartons)
    ShipmentService-->>CartonSelector: Shipment

    CartonSelector-->>PackController: MultiCartonResult
    PackController-->>PackStation: 200 OK (multiple cartons)
```

## 2. 3D Bin Packing Algorithm Flow

### Detailed 3D Packing Process

```mermaid
sequenceDiagram
    autonumber
    participant Packer
    participant BinPacker3D
    participant SpaceManager
    participant CollisionDetector
    participant Visualizer

    Packer->>BinPacker3D: pack(items, container)

    BinPacker3D->>BinPacker3D: sortItems(FFD)
    Note over BinPacker3D: Sort by volume descending

    BinPacker3D->>SpaceManager: initializeSpace(container)
    SpaceManager->>SpaceManager: createSpace(0,0,0, L,W,H)
    SpaceManager-->>BinPacker3D: AvailableSpaces

    loop For each item
        BinPacker3D->>BinPacker3D: generateOrientations(item)
        Note over BinPacker3D: 6 possible orientations

        loop For each orientation
            loop For each available space
                BinPacker3D->>CollisionDetector: canFit(item, space, orientation)

                CollisionDetector->>CollisionDetector: checkDimensions()
                CollisionDetector->>CollisionDetector: checkWeightLimit()
                CollisionDetector->>CollisionDetector: checkStackability()

                alt Item fits
                    CollisionDetector-->>BinPacker3D: true

                    BinPacker3D->>BinPacker3D: placeItem(item, space, orientation)

                    BinPacker3D->>SpaceManager: splitSpace(space, placedItem)

                    SpaceManager->>SpaceManager: createRightSpace()
                    SpaceManager->>SpaceManager: createTopSpace()
                    SpaceManager->>SpaceManager: createFrontSpace()
                    SpaceManager->>SpaceManager: removeOriginalSpace()

                    SpaceManager-->>BinPacker3D: UpdatedSpaces

                    BinPacker3D->>Visualizer: addPlacement(item, position)

                    break Item placed successfully
                end
            end
        end

        alt Item not placed
            BinPacker3D->>BinPacker3D: markAsUnpacked(item)
        end
    end

    BinPacker3D->>BinPacker3D: calculateMetrics()
    Note over BinPacker3D: Utilization, weight distribution

    BinPacker3D->>Visualizer: generate3DView()
    Visualizer-->>BinPacker3D: PackingVisualization

    BinPacker3D-->>Packer: PackingResult
```

### Space Splitting Algorithm

```mermaid
sequenceDiagram
    autonumber
    participant SpaceManager
    participant Space
    participant PlacedItem
    participant SpaceOptimizer

    SpaceManager->>Space: splitSpace(originalSpace, placedItem)

    Space->>Space: getItemBounds(placedItem)
    Note over Space: Get item's bounding box

    Space->>Space: calculateRightSpace()
    Note over Space: Space to the right of item
    alt Right space valid
        Space->>SpaceOptimizer: optimizeSpace(rightSpace)
        SpaceOptimizer->>SpaceOptimizer: mergeWithAdjacent()
        SpaceOptimizer-->>Space: OptimizedRightSpace
    end

    Space->>Space: calculateTopSpace()
    Note over Space: Space above item
    alt Top space valid
        Space->>SpaceOptimizer: optimizeSpace(topSpace)
        SpaceOptimizer-->>Space: OptimizedTopSpace
    end

    Space->>Space: calculateFrontSpace()
    Note over Space: Space in front of item
    alt Front space valid
        Space->>SpaceOptimizer: optimizeSpace(frontSpace)
        SpaceOptimizer-->>Space: OptimizedFrontSpace
    end

    Space->>SpaceManager: addNewSpaces(right, top, front)
    Space->>SpaceManager: removeOriginalSpace()

    SpaceManager->>SpaceManager: sortSpacesByVolume()
    SpaceManager->>SpaceManager: mergeAdjacentSpaces()

    SpaceManager-->>Space: UpdatedSpaceList
```

## 3. Shipping Label Generation

### Generate Shipping Label with Carrier

```mermaid
sequenceDiagram
    autonumber
    participant PackStation
    participant ShipController
    participant LabelService
    participant RateCalculator
    participant CarrierAPI
    participant ShipmentRepository

    PackStation->>ShipController: POST /ship/generate-label
    Note over PackStation: Packed carton ready

    ShipController->>LabelService: generateLabel(carton, shipment)

    LabelService->>RateCalculator: calculateRate(carton, destination)

    RateCalculator->>RateCalculator: calculateDimWeight()
    Note over RateCalculator: L × W × H ÷ 166

    RateCalculator->>RateCalculator: getChargeableWeight()
    Note over RateCalculator: Max(actual, dimensional)

    RateCalculator->>CarrierAPI: getRates(weight, destination)
    CarrierAPI-->>RateCalculator: RateOptions

    RateCalculator->>RateCalculator: selectBestRate()
    RateCalculator-->>LabelService: SelectedRate

    LabelService->>CarrierAPI: createShipment(carton, rate)

    CarrierAPI->>CarrierAPI: validateAddress()
    CarrierAPI->>CarrierAPI: assignTrackingNumber()
    CarrierAPI->>CarrierAPI: generateLabelImage()

    CarrierAPI-->>LabelService: ShippingLabel

    LabelService->>ShipmentRepository: attachLabel(carton, label)
    ShipmentRepository-->>LabelService: Updated

    LabelService->>Kafka: label.generated

    LabelService-->>ShipController: Label + TrackingNumber
    ShipController-->>PackStation: 200 OK (label ready)
```

### Multi-Carrier Rate Shopping

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant RateController
    participant RateShoppingService
    participant FedExAPI
    participant UPSAPI
    participant USPSAPI
    participant CacheService

    Client->>RateController: POST /rates/compare
    Note over Client: Shipment details

    RateController->>RateShoppingService: compareRates(shipment)

    RateShoppingService->>CacheService: getCachedRates(shipmentHash)

    alt Cache miss
        par Get FedEx Rates
            RateShoppingService->>FedExAPI: getRates(shipment)
            FedExAPI-->>RateShoppingService: FedExRates
        and Get UPS Rates
            RateShoppingService->>UPSAPI: getRates(shipment)
            UPSAPI-->>RateShoppingService: UPSRates
        and Get USPS Rates
            RateShoppingService->>USPSAPI: getRates(shipment)
            USPSAPI-->>RateShoppingService: USPSRates
        end

        RateShoppingService->>RateShoppingService: normalizeRates()
        RateShoppingService->>RateShoppingService: addTransitTimes()
        RateShoppingService->>RateShoppingService: calculateTotalCost()

        RateShoppingService->>CacheService: cacheRates(rates, 1hour)
    else Cache hit
        CacheService-->>RateShoppingService: CachedRates
    end

    RateShoppingService->>RateShoppingService: rankByValue()
    Note over RateShoppingService: Cost vs Speed analysis

    RateShoppingService-->>RateController: RateComparison
    RateController-->>Client: 200 OK (rate options)
```

## 4. Pack and Ship Workflow

### Complete Pack-Ship Process

```mermaid
sequenceDiagram
    autonumber
    participant Operator
    participant PackShipController
    participant PackService
    participant CartonSelector
    participant ShipService
    participant EventPublisher

    Operator->>PackShipController: POST /packship/process
    Note over Operator: Order ready to pack

    PackShipController->>PackService: startPacking(orderId)

    PackService->>CartonSelector: selectOptimalCarton(items)
    CartonSelector-->>PackService: SelectedCarton

    PackService->>PackService: createShipment(order, carton)
    PackService->>PackService: generatePackingSlip()

    PackService->>EventPublisher: publish(PackingStarted)

    PackService-->>PackShipController: PackingInstructions
    PackShipController-->>Operator: Show packing guide

    Operator->>PackShipController: PUT /packship/confirm-packed
    Note over Operator: Items packed, weight entered

    PackShipController->>PackService: confirmPacked(cartonId, actualWeight)

    PackService->>PackService: updateCartonWeight()
    PackService->>PackService: validateWeight()

    PackService->>ShipService: generateLabel(carton)
    ShipService-->>PackService: Label

    PackService->>EventPublisher: publish(CartonPacked)
    PackService->>EventPublisher: publish(LabelGenerated)

    PackService-->>PackShipController: Label + Instructions
    PackShipController-->>Operator: 200 OK (print label)
```

## 5. Exception Handling

### Packing Exception Flow

```mermaid
sequenceDiagram
    autonumber
    participant Operator
    participant PackController
    participant ExceptionHandler
    participant CartonSelector
    participant NotificationService
    participant SupervisorApp

    Operator->>PackController: POST /pack/exception
    Note over Operator: Items don't fit

    PackController->>ExceptionHandler: handlePackingException(items, carton, issue)

    alt Items too large
        ExceptionHandler->>CartonSelector: findLargerCarton(items)

        alt Larger carton available
            CartonSelector-->>ExceptionHandler: AlternativeCarton
            ExceptionHandler-->>PackController: UseAlternative
        else No single carton fits
            ExceptionHandler->>CartonSelector: splitIntoMultiple(items)
            CartonSelector-->>ExceptionHandler: MultipleCartons
            ExceptionHandler-->>PackController: SplitRequired
        end

    else Fragile item concern
        ExceptionHandler->>ExceptionHandler: addProtectiveMaterial()
        ExceptionHandler->>CartonSelector: selectWithExtraPadding(items)
        CartonSelector-->>ExceptionHandler: ProtectiveCarton

    else Weight exceeded
        ExceptionHandler->>CartonSelector: splitByWeight(items)
        CartonSelector-->>ExceptionHandler: WeightBalancedCartons
    end

    ExceptionHandler->>NotificationService: notifySupervisor(exception)
    NotificationService->>SupervisorApp: Alert

    ExceptionHandler-->>PackController: Resolution
    PackController-->>Operator: 200 OK (new instructions)
```

### Label Void and Regeneration

```mermaid
sequenceDiagram
    autonumber
    participant Operator
    participant ShipController
    participant LabelService
    participant CarrierAPI
    participant AuditService
    participant ShipmentRepository

    Operator->>ShipController: POST /ship/void-label
    Note over Operator: Wrong address or damaged

    ShipController->>LabelService: voidLabel(trackingNumber, reason)

    LabelService->>ShipmentRepository: findByTracking(trackingNumber)
    ShipmentRepository-->>LabelService: Shipment

    LabelService->>LabelService: validateCanVoid()
    Note over LabelService: Not shipped yet

    LabelService->>CarrierAPI: voidShipment(trackingNumber)
    CarrierAPI->>CarrierAPI: cancelLabel()
    CarrierAPI-->>LabelService: VoidConfirmation

    LabelService->>AuditService: logVoid(label, reason, operator)

    LabelService->>LabelService: markLabelVoided()
    LabelService->>ShipmentRepository: save(shipment)

    LabelService->>Kafka: label.voided

    LabelService-->>ShipController: VoidComplete
    ShipController-->>Operator: 200 OK

    Operator->>ShipController: POST /ship/generate-label
    Note over Operator: Generate new label
```

## 6. Tracking and Delivery Updates

### Shipment Tracking Updates

```mermaid
sequenceDiagram
    autonumber
    participant CarrierWebhook
    participant TrackingController
    participant TrackingService
    participant ShipmentRepository
    participant NotificationService
    participant CustomerAPI

    CarrierWebhook->>TrackingController: POST /tracking/webhook
    Note over CarrierWebhook: Status update event

    TrackingController->>TrackingService: updateTracking(trackingNumber, status)

    TrackingService->>ShipmentRepository: findByTracking(trackingNumber)
    ShipmentRepository-->>TrackingService: Shipment

    TrackingService->>TrackingService: validateUpdate(status)

    alt In Transit
        TrackingService->>TrackingService: updateLocation(scan)
        TrackingService->>TrackingService: calculateETA()
    else Out for Delivery
        TrackingService->>NotificationService: notifyCustomer(delivery today)
    else Delivered
        TrackingService->>TrackingService: recordDelivery(signature, time)
        TrackingService->>NotificationService: notifyDelivered()
    else Exception
        TrackingService->>TrackingService: handleException(type)
        TrackingService->>NotificationService: alertCustomerService()
    end

    TrackingService->>ShipmentRepository: save(shipment)

    TrackingService->>Kafka: tracking.updated

    TrackingService->>CustomerAPI: updateOrderStatus(orderId, status)

    TrackingService-->>TrackingController: UpdateConfirmed
    TrackingController-->>CarrierWebhook: 200 OK
```

## 7. Analytics and Optimization

### Packing Efficiency Analysis

```mermaid
sequenceDiagram
    autonumber
    participant Analytics
    participant AnalyticsController
    participant PackingAnalyzer
    participant ShipmentRepository
    participant OptimizationEngine

    Analytics->>AnalyticsController: GET /analytics/packing-efficiency
    Note over Analytics: Date range

    AnalyticsController->>PackingAnalyzer: analyzeEfficiency(dateRange)

    PackingAnalyzer->>ShipmentRepository: findShipments(dateRange)
    ShipmentRepository-->>PackingAnalyzer: List<Shipment>

    loop For each shipment
        PackingAnalyzer->>PackingAnalyzer: calculateFillRate()
        PackingAnalyzer->>PackingAnalyzer: calculateDimWeightRatio()
        PackingAnalyzer->>PackingAnalyzer: calculateCostEfficiency()
    end

    PackingAnalyzer->>OptimizationEngine: identifyPatterns()

    OptimizationEngine->>OptimizationEngine: findOverpackaging()
    OptimizationEngine->>OptimizationEngine: findUnderUtilization()
    OptimizationEngine->>OptimizationEngine: suggestCartonChanges()

    OptimizationEngine-->>PackingAnalyzer: Recommendations

    PackingAnalyzer->>PackingAnalyzer: generateReport()

    PackingAnalyzer-->>AnalyticsController: EfficiencyReport
    AnalyticsController-->>Analytics: 200 OK (report)
```

## Error Recovery Patterns

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant PackShipController
    participant PackService
    participant CircuitBreaker
    participant FallbackService
    participant ErrorLogger

    Client->>PackShipController: POST /pack/process

    PackShipController->>PackService: processPacking(order)

    PackService->>CircuitBreaker: checkCarrierHealth()

    alt Circuit Open (Carrier API Down)
        CircuitBreaker-->>PackService: CircuitOpen

        PackService->>FallbackService: useOfflineMode()
        FallbackService->>FallbackService: generateOfflineLabel()
        FallbackService->>FallbackService: queueForLaterSync()

        FallbackService-->>PackService: OfflineLabel

        PackService->>ErrorLogger: logCircuitOpen()
    else Circuit Closed
        CircuitBreaker-->>PackService: Healthy

        PackService->>CarrierAPI: generateLabel()

        alt API Timeout
            CarrierAPI--x PackService: Timeout

            PackService->>CircuitBreaker: recordFailure()
            PackService->>FallbackService: useOfflineMode()
        else Success
            CarrierAPI-->>PackService: Label
        end
    end

    PackService-->>PackShipController: Result
    PackShipController-->>Client: Response
```

## Key Patterns

1. **3D Bin Packing**: FFD algorithm with 6 orientations and space splitting
2. **Multi-Criteria Scoring**: Weighted scoring for carton selection
3. **Rate Shopping**: Parallel carrier API calls with caching
4. **Exception Handling**: Graceful degradation with offline mode
5. **Real-time Tracking**: Webhook-based status updates
6. **Analytics**: Continuous optimization based on historical data
7. **Audit Trail**: Complete logging of all packing decisions