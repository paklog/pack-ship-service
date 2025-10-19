package com.paklog.wes.pack.domain.valueobject;

/**
 * Container types with standard dimensions
 */
public enum ContainerType {
    SMALL_BOX(12, 9, 6, 20.0, "Small Box"),
    MEDIUM_BOX(18, 14, 12, 40.0, "Medium Box"),
    LARGE_BOX(24, 18, 18, 60.0, "Large Box"),
    EXTRA_LARGE_BOX(36, 24, 24, 70.0, "Extra Large Box"),
    PALLET(48, 40, 48, 2000.0, "Pallet"),
    TOTE(23, 15, 12, 30.0, "Tote"),
    CUSTOM(0, 0, 0, 0.0, "Custom");

    private final double length; // inches
    private final double width;  // inches
    private final double height; // inches
    private final double maxWeightLb; // pounds
    private final String displayName;

    ContainerType(double length, double width, double height, double maxWeightLb, String displayName) {
        this.length = length;
        this.width = width;
        this.height = height;
        this.maxWeightLb = maxWeightLb;
        this.displayName = displayName;
    }

    public double getVolumeCubicInches() {
        return length * width * height;
    }

    public double getLength() {
        return length;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getMaxWeightLb() {
        return maxWeightLb;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canHold(double weightLb, double volumeCubicInches) {
        if (this == CUSTOM) {
            return true; // Custom containers have no predefined limits
        }
        return weightLb <= maxWeightLb && volumeCubicInches <= getVolumeCubicInches();
    }
}
