package com.paklog.wes.pack.domain.entity;

import com.paklog.wes.pack.domain.valueobject.ContainerType;
import com.paklog.wes.pack.domain.valueobject.Dimensions;
import com.paklog.wes.pack.domain.valueobject.Weight;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Container entity - physical container for packing items
 */
public class Container {

    public enum ContainerStatus {
        OPEN,
        SEALING,
        SEALED,
        LABELED,
        SHIPPED
    }

    private String containerId;
    private ContainerType type;
    private Dimensions dimensions;
    private Weight weight;
    private Weight maxWeight;
    private ContainerStatus status;
    private List<String> itemInstructionIds;
    private LocalDateTime createdAt;
    private LocalDateTime sealedAt;
    private String notes;

    public Container() {
        // For persistence
        this.itemInstructionIds = new ArrayList<>();
    }

    public static Container create(ContainerType type) {
        Container container = new Container();
        container.containerId = generateContainerId();
        container.type = Objects.requireNonNull(type, "Container type cannot be null");
        container.dimensions = new Dimensions(
                type.getLength(),
                type.getWidth(),
                type.getHeight(),
                Dimensions.DimensionUnit.IN
        );
        container.weight = new Weight(0.0, Weight.WeightUnit.LB);
        container.maxWeight = new Weight(type.getMaxWeightLb(), Weight.WeightUnit.LB);
        container.status = ContainerStatus.OPEN;
        container.itemInstructionIds = new ArrayList<>();
        container.createdAt = LocalDateTime.now();
        return container;
    }

    public static Container createCustom(Dimensions dimensions, Weight maxWeight) {
        Container container = new Container();
        container.containerId = generateContainerId();
        container.type = ContainerType.CUSTOM;
        container.dimensions = Objects.requireNonNull(dimensions, "Dimensions cannot be null");
        container.weight = new Weight(0.0, Weight.WeightUnit.LB);
        container.maxWeight = Objects.requireNonNull(maxWeight, "Max weight cannot be null");
        container.status = ContainerStatus.OPEN;
        container.itemInstructionIds = new ArrayList<>();
        container.createdAt = LocalDateTime.now();
        return container;
    }

    /**
     * Add item to container
     */
    public void addItem(String instructionId, Weight itemWeight) {
        if (status != ContainerStatus.OPEN) {
            throw new IllegalStateException("Cannot add items to " + status + " container");
        }

        Objects.requireNonNull(instructionId, "Instruction ID cannot be null");
        Objects.requireNonNull(itemWeight, "Item weight cannot be null");

        // Check weight capacity
        Weight newWeight = this.weight.add(itemWeight);
        if (newWeight.isGreaterThan(maxWeight)) {
            throw new IllegalStateException("Container would exceed maximum weight");
        }

        this.itemInstructionIds.add(instructionId);
        this.weight = newWeight;
    }

    /**
     * Seal the container
     */
    public void seal(Weight actualWeight) {
        if (status != ContainerStatus.OPEN) {
            throw new IllegalStateException("Can only seal open containers");
        }

        if (itemInstructionIds.isEmpty()) {
            throw new IllegalStateException("Cannot seal empty container");
        }

        Objects.requireNonNull(actualWeight, "Actual weight cannot be null");

        this.weight = actualWeight;
        this.status = ContainerStatus.SEALED;
        this.sealedAt = LocalDateTime.now();
    }

    /**
     * Mark container as labeled
     */
    public void markLabeled() {
        if (status != ContainerStatus.SEALED) {
            throw new IllegalStateException("Can only label sealed containers");
        }
        this.status = ContainerStatus.LABELED;
    }

    /**
     * Mark container as shipped
     */
    public void markShipped() {
        if (status != ContainerStatus.LABELED) {
            throw new IllegalStateException("Can only ship labeled containers");
        }
        this.status = ContainerStatus.SHIPPED;
    }

    /**
     * Calculate utilization percentage
     */
    public double getUtilizationPercentage() {
        if (maxWeight.value() == 0) {
            return 0.0;
        }
        return (weight.toPounds() / maxWeight.toPounds()) * 100.0;
    }

    /**
     * Get remaining capacity
     */
    public Weight getRemainingCapacity() {
        double remainingLb = maxWeight.toPounds() - weight.toPounds();
        return new Weight(Math.max(0, remainingLb), Weight.WeightUnit.LB);
    }

    /**
     * Check if container can hold additional weight
     */
    public boolean canHold(Weight additionalWeight) {
        Weight newWeight = this.weight.add(additionalWeight);
        return !newWeight.isGreaterThan(maxWeight);
    }

    /**
     * Get item count
     */
    public int getItemCount() {
        return itemInstructionIds.size();
    }

    private static String generateContainerId() {
        return "CONT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters and setters

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = dimensions;
    }

    public Weight getWeight() {
        return weight;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }

    public Weight getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(Weight maxWeight) {
        this.maxWeight = maxWeight;
    }

    public ContainerStatus getStatus() {
        return status;
    }

    public void setStatus(ContainerStatus status) {
        this.status = status;
    }

    public List<String> getItemInstructionIds() {
        return itemInstructionIds;
    }

    public void setItemInstructionIds(List<String> itemInstructionIds) {
        this.itemInstructionIds = itemInstructionIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(LocalDateTime sealedAt) {
        this.sealedAt = sealedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Container container = (Container) o;
        return Objects.equals(containerId, container.containerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containerId);
    }

    @Override
    public String toString() {
        return "Container{" +
                "containerId='" + containerId + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", itemCount=" + getItemCount() +
                ", utilization=" + String.format("%.1f%%", getUtilizationPercentage()) +
                '}';
    }
}
