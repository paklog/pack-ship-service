package com.paklog.wes.pack.domain.aggregate;

import com.paklog.domain.annotation.AggregateRoot;
import com.paklog.domain.shared.DomainEvent;
import com.paklog.wes.pack.domain.entity.*;
import com.paklog.wes.pack.domain.event.*;
import com.paklog.wes.pack.domain.exception.*;
import com.paklog.wes.pack.domain.valueobject.ContainerType;
import com.paklog.wes.pack.domain.valueobject.PackingStatus;
import com.paklog.wes.pack.domain.valueobject.Weight;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * PackingSession aggregate root
 * Manages the complete packing workflow
 */
@AggregateRoot
@Document(collection = "packing_sessions")
public class PackingSession {

    @Id
    private String sessionId;

    @Version
    private Long version;

    private String pickSessionId;
    private String orderId;
    private String workerId;
    private String warehouseId;
    private PackingStatus status;
    private List<PackingInstruction> packingInstructions;
    private List<Container> containers;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String cancellationReason;

    // Sprint 1: Scanning workflow
    private String stationId;
    private String packerId;
    private List<ItemToScan> itemsToScan;
    private List<ScannedItem> scannedItems;

    // Sprint 1: Carton selection
    private String recommendedCarton;
    private String selectedCarton;

    // Sprint 1: Weight verification
    private Weight estimatedWeight;
    private Weight actualWeight;

    // Sprint 1: Quality control
    private QualityCheck qualityCheck;

    // Sprint 1: Packing materials
    private List<PackingMaterial> packingMaterials;

    private List<DomainEvent> domainEvents = new ArrayList<>();

    public PackingSession() {
        // For MongoDB/persistence
        this.packingInstructions = new ArrayList<>();
        this.containers = new ArrayList<>();
        this.itemsToScan = new ArrayList<>();
        this.scannedItems = new ArrayList<>();
        this.packingMaterials = new ArrayList<>();
    }

    /**
     * Create a new packing session
     */
    public static PackingSession create(
            String pickSessionId,
            String orderId,
            String workerId,
            String warehouseId,
            List<PackingInstruction> instructions
    ) {
        PackingSession session = new PackingSession();
        session.sessionId = generateSessionId();
        session.pickSessionId = Objects.requireNonNull(pickSessionId, "Pick session ID cannot be null");
        session.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        session.workerId = Objects.requireNonNull(workerId, "Worker ID cannot be null");
        session.warehouseId = Objects.requireNonNull(warehouseId, "Warehouse ID cannot be null");
        session.packingInstructions = new ArrayList<>(Objects.requireNonNull(instructions, "Instructions cannot be null"));
        session.containers = new ArrayList<>();
        session.itemsToScan = new ArrayList<>();
        session.scannedItems = new ArrayList<>();
        session.packingMaterials = new ArrayList<>();
        session.status = PackingStatus.CREATED;
        session.createdAt = LocalDateTime.now();

        if (session.packingInstructions.isEmpty()) {
            throw new IllegalArgumentException("Session must have at least one packing instruction");
        }

        return session;
    }

    /**
     * Start the packing session
     */
    public void start() {
        ensureStatus(PackingStatus.CREATED);

        this.status = PackingStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();

        registerEvent(new PackingSessionStartedEvent(
                this.sessionId,
                this.pickSessionId,
                this.orderId,
                this.workerId,
                this.warehouseId,
                this.packingInstructions.size()
        ));
    }

    /**
     * Pack item into container
     */
    public void packItem(String instructionId, String containerId, int quantity) {
        ensureStatus(PackingStatus.IN_PROGRESS);

        PackingInstruction instruction = findInstruction(instructionId);
        Container container = findOrCreateContainer(containerId);

        // Auto-start instruction if pending
        if (instruction.getStatus() == PackingInstruction.InstructionStatus.PENDING) {
            instruction.start();
        }

        // Pack the item
        instruction.pack(containerId, quantity);

        // Add weight to container
        Weight itemWeight = instruction.getTotalWeight();
        container.addItem(instructionId, itemWeight);

        registerEvent(new ItemPackedEvent(
                this.sessionId,
                instructionId,
                instruction.getItemSku(),
                containerId,
                quantity,
                this.workerId
        ));

        // Check if all items packed - auto-complete
        if (allInstructionsPacked()) {
            complete();
        }
    }

    /**
     * Seal a container
     */
    public void sealContainer(String containerId, Weight actualWeight) {
        ensureStatus(PackingStatus.IN_PROGRESS);

        Container container = findContainer(containerId);
        container.seal(actualWeight);

        registerEvent(new ContainerSealedEvent(
                this.sessionId,
                containerId,
                container.getItemCount(),
                actualWeight.toPounds(),
                this.workerId
        ));
    }

    /**
     * Mark instruction as missing
     */
    public void markItemMissing(String instructionId, String reason) {
        ensureStatus(PackingStatus.IN_PROGRESS);

        PackingInstruction instruction = findInstruction(instructionId);
        instruction.markMissing(reason);

        // Check if all items processed
        if (allInstructionsPacked()) {
            complete();
        }
    }

    /**
     * Mark instruction as damaged
     */
    public void markItemDamaged(String instructionId, String reason) {
        ensureStatus(PackingStatus.IN_PROGRESS);

        PackingInstruction instruction = findInstruction(instructionId);
        instruction.markDamaged(reason);

        // Check if all items processed
        if (allInstructionsPacked()) {
            complete();
        }
    }

    // ========== Sprint 1: Scanning Workflow ==========

    /**
     * Initialize items to scan from packing instructions
     */
    public void initializeItemsToScan() {
        ensureStatus(PackingStatus.CREATED);

        for (PackingInstruction instruction : packingInstructions) {
            ItemToScan item = new ItemToScan(
                    instruction.getItemSku(),
                    instruction.getBarcode(),
                    instruction.getExpectedQuantity()
            );
            itemsToScan.add(item);
        }

        this.status = PackingStatus.SCANNING;

        registerEvent(new PackingSessionStartedEvent(
                this.sessionId,
                this.pickSessionId,
                this.orderId,
                this.workerId,
                this.warehouseId,
                this.packingInstructions.size()
        ));
    }

    /**
     * Scan an item by barcode
     */
    public void scanItem(String barcode) {
        ensureStatus(PackingStatus.SCANNING);

        // Find the item to scan
        ItemToScan itemToScan = itemsToScan.stream()
                .filter(item -> item.getBarcode().equals(barcode))
                .findFirst()
                .orElseThrow(() -> new UnexpectedItemException(barcode));

        // Check if already fully scanned
        if (itemToScan.isFullyScanned()) {
            throw new AlreadyScannedException(
                    barcode,
                    itemToScan.getScannedQuantity(),
                    itemToScan.getExpectedQuantity()
            );
        }

        // Mark as scanned
        itemToScan.markScanned();

        // Record the scan
        ScannedItem scannedItem = new ScannedItem(itemToScan, LocalDateTime.now());
        scannedItem.setScannedBy(this.packerId != null ? this.packerId : this.workerId);
        scannedItems.add(scannedItem);

        // Check if all items scanned
        if (allItemsScanned()) {
            this.status = PackingStatus.READY_FOR_CARTON;
        }
    }

    /**
     * Check if all items have been scanned
     */
    public boolean allItemsScanned() {
        return itemsToScan.stream().allMatch(ItemToScan::isFullyScanned);
    }

    /**
     * Get scanning progress percentage
     */
    public double getScanningProgress() {
        if (itemsToScan.isEmpty()) {
            return 100.0;
        }
        long scannedCount = itemsToScan.stream()
                .filter(ItemToScan::isFullyScanned)
                .count();
        return (scannedCount / (double) itemsToScan.size()) * 100.0;
    }

    // ========== Sprint 1: Carton Selection ==========

    /**
     * Recommend a carton based on items
     */
    public String recommendCarton() {
        ensureStatus(PackingStatus.READY_FOR_CARTON);

        // Simple recommendation based on item count
        int totalItems = packingInstructions.stream()
                .mapToInt(PackingInstruction::getExpectedQuantity)
                .sum();

        if (totalItems <= 5) {
            this.recommendedCarton = "SMALL_BOX";
        } else if (totalItems <= 15) {
            this.recommendedCarton = "MEDIUM_BOX";
        } else {
            this.recommendedCarton = "LARGE_BOX";
        }

        return this.recommendedCarton;
    }

    /**
     * Select a carton for packing
     */
    public void selectCarton(String cartonType) {
        ensureStatus(PackingStatus.READY_FOR_CARTON);

        if (!isCartonSuitable(cartonType)) {
            throw new UnsuitableCartonException(
                    ContainerType.valueOf(cartonType),
                    "Carton too small for items"
            );
        }

        this.selectedCarton = cartonType;
        this.status = PackingStatus.READY_TO_PACK;

        // Calculate packing materials needed
        calculatePackingMaterials();
    }

    /**
     * Check if carton is suitable for items
     */
    public boolean isCartonSuitable(String cartonType) {
        int totalItems = packingInstructions.stream()
                .mapToInt(PackingInstruction::getExpectedQuantity)
                .sum();

        return switch (cartonType) {
            case "SMALL_BOX" -> totalItems <= 5;
            case "MEDIUM_BOX" -> totalItems <= 15;
            case "LARGE_BOX" -> totalItems <= 30;
            default -> false;
        };
    }

    // ========== Sprint 1: Packing Materials ==========

    /**
     * Calculate packing materials needed
     */
    public void calculatePackingMaterials() {
        this.packingMaterials.clear();

        // Add tape (always needed)
        packingMaterials.add(new PackingMaterial(PackingMaterial.MaterialType.TAPE, 6.0));

        // Add bubble wrap based on carton size
        double bubbleWrapFeet = switch (selectedCarton) {
            case "SMALL_BOX" -> 3.0;
            case "MEDIUM_BOX" -> 6.0;
            case "LARGE_BOX" -> 10.0;
            default -> 5.0;
        };
        packingMaterials.add(new PackingMaterial(PackingMaterial.MaterialType.BUBBLE_WRAP, bubbleWrapFeet));

        // Add packing paper
        packingMaterials.add(new PackingMaterial(PackingMaterial.MaterialType.PACKING_PAPER, 4.0));
    }

    // ========== Sprint 1: Weight Verification ==========

    /**
     * Weigh and close the package
     */
    public void weighAndClose(Weight actualWeight) {
        ensureStatus(PackingStatus.IN_PROGRESS, PackingStatus.READY_TO_WEIGH);

        this.actualWeight = actualWeight;

        // Calculate estimated weight from instructions
        this.estimatedWeight = new Weight(
                packingInstructions.stream()
                        .mapToDouble(i -> i.getTotalWeight().toPounds())
                        .sum()
        );

        // Check weight discrepancy (5% tolerance)
        double discrepancy = Math.abs(actualWeight.toPounds() - estimatedWeight.toPounds()) / estimatedWeight.toPounds();

        if (discrepancy > 0.05) {
            throw new WeightDiscrepancyException(estimatedWeight, actualWeight, discrepancy);
        }

        this.status = PackingStatus.READY_TO_SHIP;
    }

    // ========== Sprint 1: Quality Control ==========

    /**
     * Perform quality check
     */
    public void performQualityCheck(String checkerId, List<QualityCheck.Checkpoint> checkpoints) {
        ensureStatus(PackingStatus.IN_PROGRESS);

        this.qualityCheck = new QualityCheck(checkerId, checkpoints, LocalDateTime.now());

        if (qualityCheck.isPassed()) {
            this.status = PackingStatus.QC_PASSED;
        } else {
            this.status = PackingStatus.QC_FAILED;
            handleQualityFailure();
        }
    }

    /**
     * Perform quality check with photos
     */
    public void performQualityCheck(String checkerId, List<QualityCheck.Checkpoint> checkpoints, List<String> photoUrls) {
        ensureStatus(PackingStatus.IN_PROGRESS);

        this.qualityCheck = new QualityCheck(checkerId, checkpoints, photoUrls, LocalDateTime.now());

        if (qualityCheck.isPassed()) {
            this.status = PackingStatus.QC_PASSED;
        } else {
            this.status = PackingStatus.QC_FAILED;
            handleQualityFailure();
        }
    }

    /**
     * Handle quality check failure
     */
    private void handleQualityFailure() {
        // Reset to packing state for rework
        this.status = PackingStatus.PACKING;
    }

    /**
     * Complete the packing session
     */
    public void complete() {
        ensureStatus(PackingStatus.IN_PROGRESS);

        if (!allInstructionsPacked()) {
            throw new IllegalStateException("Cannot complete session with pending instructions");
        }

        this.status = PackingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        registerEvent(new PackingSessionCompletedEvent(
                this.sessionId,
                this.orderId,
                this.workerId,
                this.warehouseId,
                this.packingInstructions.size(),
                getPackedInstructionCount(),
                this.containers.size(),
                getTotalWeight(),
                calculateAccuracy(),
                getDuration()
        ));
    }

    /**
     * Cancel the packing session
     */
    public void cancel(String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel session in terminal state: " + status);
        }

        this.status = PackingStatus.CANCELLED;
        this.cancellationReason = reason;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Add new container to session
     */
    public Container addContainer(Container container) {
        Objects.requireNonNull(container, "Container cannot be null");
        this.containers.add(container);
        return container;
    }

    /**
     * Get current (next) instruction to pack
     */
    public PackingInstruction getCurrentInstruction() {
        return packingInstructions.stream()
                .filter(i -> !i.isComplete())
                .findFirst()
                .orElse(null);
    }

    /**
     * Calculate packing progress
     */
    public double getProgress() {
        if (packingInstructions.isEmpty()) {
            return 100.0;
        }
        return (getPackedInstructionCount() / (double) packingInstructions.size()) * 100.0;
    }

    /**
     * Calculate packing accuracy
     */
    public double calculateAccuracy() {
        if (packingInstructions.isEmpty()) {
            return 100.0;
        }

        long packedCount = packingInstructions.stream()
                .filter(i -> i.getStatus() == PackingInstruction.InstructionStatus.PACKED)
                .count();

        return (packedCount / (double) packingInstructions.size()) * 100.0;
    }

    /**
     * Get total weight of all containers
     */
    public double getTotalWeight() {
        return containers.stream()
                .mapToDouble(c -> c.getWeight().toPounds())
                .sum();
    }

    /**
     * Get session duration
     */
    public Duration getDuration() {
        if (startedAt == null) {
            return Duration.ZERO;
        }
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return Duration.between(startedAt, endTime);
    }

    /**
     * Get packed instruction count
     */
    public int getPackedInstructionCount() {
        return (int) packingInstructions.stream()
                .filter(PackingInstruction::isComplete)
                .count();
    }

    // Private helper methods

    private boolean allInstructionsPacked() {
        return packingInstructions.stream().allMatch(PackingInstruction::isComplete);
    }

    private Container findOrCreateContainer(String containerId) {
        return containers.stream()
                .filter(c -> c.getContainerId().equals(containerId))
                .findFirst()
                .orElse(null); // Will be null if new container needed
    }

    private Container findContainer(String containerId) {
        return containers.stream()
                .filter(c -> c.getContainerId().equals(containerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + containerId));
    }

    private PackingInstruction findInstruction(String instructionId) {
        return packingInstructions.stream()
                .filter(i -> i.getInstructionId().equals(instructionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Instruction not found: " + instructionId));
    }

    private void ensureStatus(PackingStatus... allowedStatuses) {
        for (PackingStatus allowed : allowedStatuses) {
            if (this.status == allowed) {
                return;
            }
        }
        throw new IllegalStateException(
                String.format("Invalid status transition. Current: %s, Expected: %s",
                        this.status, List.of(allowedStatuses))
        );
    }

    private void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    private static String generateSessionId() {
        return "PACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters and setters

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getPickSessionId() {
        return pickSessionId;
    }

    public void setPickSessionId(String pickSessionId) {
        this.pickSessionId = pickSessionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public PackingStatus getStatus() {
        return status;
    }

    public void setStatus(PackingStatus status) {
        this.status = status;
    }

    public List<PackingInstruction> getPackingInstructions() {
        return packingInstructions;
    }

    public void setPackingInstructions(List<PackingInstruction> packingInstructions) {
        this.packingInstructions = packingInstructions;
    }

    public List<Container> getContainers() {
        return containers;
    }

    public void setContainers(List<Container> containers) {
        this.containers = containers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public List<DomainEvent> getDomainEvents() {
        return domainEvents;
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Sprint 1 Getters and Setters

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getPackerId() {
        return packerId;
    }

    public void setPackerId(String packerId) {
        this.packerId = packerId;
    }

    public List<ItemToScan> getItemsToScan() {
        return itemsToScan;
    }

    public void setItemsToScan(List<ItemToScan> itemsToScan) {
        this.itemsToScan = itemsToScan;
    }

    public List<ScannedItem> getScannedItems() {
        return scannedItems;
    }

    public void setScannedItems(List<ScannedItem> scannedItems) {
        this.scannedItems = scannedItems;
    }

    public String getRecommendedCarton() {
        return recommendedCarton;
    }

    public void setRecommendedCarton(String recommendedCarton) {
        this.recommendedCarton = recommendedCarton;
    }

    public String getSelectedCarton() {
        return selectedCarton;
    }

    public void setSelectedCarton(String selectedCarton) {
        this.selectedCarton = selectedCarton;
    }

    public Weight getEstimatedWeight() {
        return estimatedWeight;
    }

    public void setEstimatedWeight(Weight estimatedWeight) {
        this.estimatedWeight = estimatedWeight;
    }

    public Weight getActualWeight() {
        return actualWeight;
    }

    public void setActualWeight(Weight actualWeight) {
        this.actualWeight = actualWeight;
    }

    public QualityCheck getQualityCheck() {
        return qualityCheck;
    }

    public void setQualityCheck(QualityCheck qualityCheck) {
        this.qualityCheck = qualityCheck;
    }

    public List<PackingMaterial> getPackingMaterials() {
        return packingMaterials;
    }

    public void setPackingMaterials(List<PackingMaterial> packingMaterials) {
        this.packingMaterials = packingMaterials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PackingSession that = (PackingSession) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "PackingSession{" +
                "sessionId='" + sessionId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", workerId='" + workerId + '\'' +
                ", status=" + status +
                ", instructionCount=" + packingInstructions.size() +
                ", containerCount=" + containers.size() +
                ", progress=" + String.format("%.1f%%", getProgress()) +
                '}';
    }
}
