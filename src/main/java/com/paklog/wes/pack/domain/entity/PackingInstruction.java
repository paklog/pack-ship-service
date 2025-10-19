package com.paklog.wes.pack.domain.entity;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.pack.domain.valueobject.Dimensions;
import com.paklog.wes.pack.domain.valueobject.Weight;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Packing instruction entity - item to be packed
 */
public class PackingInstruction {

    public enum InstructionStatus {
        PENDING,
        IN_PROGRESS,
        PACKED,
        MISSING,
        DAMAGED,
        CANCELLED;

        public boolean isTerminal() {
            return this == PACKED || this == MISSING || this == DAMAGED || this == CANCELLED;
        }
    }

    private String instructionId;
    private String itemSku;
    private String barcode;
    private String itemDescription;
    private int expectedQuantity;
    private int packedQuantity;
    private InstructionStatus status;
    private String containerId;
    private Priority priority;
    private Weight itemWeight;
    private Dimensions itemDimensions;
    private String orderId;
    private String pickInstructionId;
    private LocalDateTime packedAt;
    private String notes;

    public PackingInstruction() {
        // For persistence
    }

    public PackingInstruction(
            String instructionId,
            String itemSku,
            String itemDescription,
            int expectedQuantity,
            Weight itemWeight,
            Dimensions itemDimensions,
            String orderId,
            Priority priority
    ) {
        this.instructionId = Objects.requireNonNull(instructionId, "Instruction ID cannot be null");
        this.itemSku = Objects.requireNonNull(itemSku, "SKU cannot be null");
        this.itemDescription = itemDescription;
        this.expectedQuantity = expectedQuantity;
        this.packedQuantity = 0;
        this.status = InstructionStatus.PENDING;
        this.itemWeight = itemWeight;
        this.itemDimensions = itemDimensions;
        this.orderId = orderId;
        this.priority = priority != null ? priority : Priority.NORMAL;
    }

    /**
     * Start packing this instruction
     */
    public void start() {
        if (status != InstructionStatus.PENDING) {
            throw new IllegalStateException("Can only start pending instructions");
        }
        this.status = InstructionStatus.IN_PROGRESS;
    }

    /**
     * Pack item into container
     */
    public void pack(String containerId, int quantity) {
        if (status != InstructionStatus.IN_PROGRESS && status != InstructionStatus.PENDING) {
            throw new IllegalStateException("Cannot pack instruction in status: " + status);
        }

        if (quantity > expectedQuantity) {
            throw new IllegalArgumentException("Packed quantity exceeds expected");
        }

        this.containerId = Objects.requireNonNull(containerId, "Container ID cannot be null");
        this.packedQuantity = quantity;
        this.status = InstructionStatus.PACKED;
        this.packedAt = LocalDateTime.now();
    }

    /**
     * Mark item as missing
     */
    public void markMissing(String reason) {
        this.status = InstructionStatus.MISSING;
        this.notes = reason;
    }

    /**
     * Mark item as damaged
     */
    public void markDamaged(String reason) {
        this.status = InstructionStatus.DAMAGED;
        this.notes = reason;
    }

    /**
     * Cancel instruction
     */
    public void cancel(String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel completed instruction");
        }
        this.status = InstructionStatus.CANCELLED;
        this.notes = reason;
    }

    /**
     * Check if instruction is complete
     */
    public boolean isComplete() {
        return status == InstructionStatus.PACKED ||
               status == InstructionStatus.MISSING ||
               status == InstructionStatus.DAMAGED ||
               status == InstructionStatus.CANCELLED;
    }

    /**
     * Get total weight for this instruction
     */
    public Weight getTotalWeight() {
        if (itemWeight == null) {
            return new Weight(0);
        }
        return new Weight(itemWeight.value() * packedQuantity, itemWeight.unit());
    }

    // Getters and setters

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    public String getItemSku() {
        return itemSku;
    }

    public void setItemSku(String itemSku) {
        this.itemSku = itemSku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public int getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(int expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public int getPackedQuantity() {
        return packedQuantity;
    }

    public void setPackedQuantity(int packedQuantity) {
        this.packedQuantity = packedQuantity;
    }

    public InstructionStatus getStatus() {
        return status;
    }

    public void setStatus(InstructionStatus status) {
        this.status = status;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Weight getItemWeight() {
        return itemWeight;
    }

    public void setItemWeight(Weight itemWeight) {
        this.itemWeight = itemWeight;
    }

    public Dimensions getItemDimensions() {
        return itemDimensions;
    }

    public void setItemDimensions(Dimensions itemDimensions) {
        this.itemDimensions = itemDimensions;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPickInstructionId() {
        return pickInstructionId;
    }

    public void setPickInstructionId(String pickInstructionId) {
        this.pickInstructionId = pickInstructionId;
    }

    public LocalDateTime getPackedAt() {
        return packedAt;
    }

    public void setPackedAt(LocalDateTime packedAt) {
        this.packedAt = packedAt;
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
        PackingInstruction that = (PackingInstruction) o;
        return Objects.equals(instructionId, that.instructionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instructionId);
    }

    // Helper for status
    public static class InstructionStatusHelper {
        public static boolean isTerminal(InstructionStatus status) {
            return status == InstructionStatus.PACKED ||
                   status == InstructionStatus.MISSING ||
                   status == InstructionStatus.DAMAGED ||
                   status == InstructionStatus.CANCELLED;
        }
    }
}
