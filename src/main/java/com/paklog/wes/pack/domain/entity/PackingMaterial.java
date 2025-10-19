package com.paklog.wes.pack.domain.entity;

import java.util.Objects;

/**
 * Packing materials required for a shipment
 */
public class PackingMaterial {

    public enum MaterialType {
        BUBBLE_WRAP("Bubble Wrap", "ft"),
        PACKING_PAPER("Packing Paper", "sheets"),
        AIR_PILLOWS("Air Pillows", "units"),
        FOAM_PEANUTS("Foam Peanuts", "oz"),
        TAPE("Packing Tape", "ft"),
        FRAGILE_STICKERS("Fragile Stickers", "units");

        private final String displayName;
        private final String unit;

        MaterialType(String displayName, String unit) {
            this.displayName = displayName;
            this.unit = unit;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getUnit() {
            return unit;
        }
    }

    private MaterialType type;
    private double quantity;
    private String unit;

    public PackingMaterial() {
        // For persistence
    }

    public PackingMaterial(MaterialType type, double quantity) {
        this.type = Objects.requireNonNull(type, "Material type cannot be null");
        this.quantity = quantity;
        this.unit = type.getUnit();
    }

    // Getters and setters

    public MaterialType getType() {
        return type;
    }

    public void setType(MaterialType type) {
        this.type = type;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return String.format("%.1f %s of %s", quantity, unit, type.getDisplayName());
    }
}
