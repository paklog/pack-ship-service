package com.paklog.wes.pack.domain.valueobject;

import java.util.Objects;

/**
 * Dimensions value object with unit conversion
 */
public record Dimensions(double length, double width, double height, DimensionUnit unit) {

    public enum DimensionUnit {
        IN("Inches"),
        CM("Centimeters"),
        FT("Feet"),
        M("Meters");

        private final String displayName;

        DimensionUnit(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Dimensions {
        if (length <= 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("All dimensions must be positive");
        }
        Objects.requireNonNull(unit, "Dimension unit cannot be null");
    }

    public Dimensions(double length, double width, double height) {
        this(length, width, height, DimensionUnit.IN);
    }

    public double getVolumeInCubicInches() {
        double inchLength = toInches(length);
        double inchWidth = toInches(width);
        double inchHeight = toInches(height);
        return inchLength * inchWidth * inchHeight;
    }

    public double getVolumeInCubicFeet() {
        return getVolumeInCubicInches() / 1728.0; // 12^3
    }

    private double toInches(double value) {
        return switch (unit) {
            case IN -> value;
            case CM -> value / 2.54;
            case FT -> value * 12.0;
            case M -> value * 39.3701;
        };
    }

    public Dimensions convertTo(DimensionUnit targetUnit) {
        if (unit == targetUnit) {
            return this;
        }

        double inchLength = toInches(length);
        double inchWidth = toInches(width);
        double inchHeight = toInches(height);

        return switch (targetUnit) {
            case IN -> new Dimensions(inchLength, inchWidth, inchHeight, DimensionUnit.IN);
            case CM -> new Dimensions(inchLength * 2.54, inchWidth * 2.54, inchHeight * 2.54, DimensionUnit.CM);
            case FT -> new Dimensions(inchLength / 12.0, inchWidth / 12.0, inchHeight / 12.0, DimensionUnit.FT);
            case M -> new Dimensions(inchLength / 39.3701, inchWidth / 39.3701, inchHeight / 39.3701, DimensionUnit.M);
        };
    }

    public boolean fitsInside(Dimensions container) {
        double[] thisDims = getSortedDimensions();
        double[] containerDims = container.getSortedDimensions();

        return thisDims[0] <= containerDims[0] &&
               thisDims[1] <= containerDims[1] &&
               thisDims[2] <= containerDims[2];
    }

    private double[] getSortedDimensions() {
        double[] dims = {toInches(length), toInches(width), toInches(height)};
        java.util.Arrays.sort(dims);
        return dims;
    }

    public String toDisplayString() {
        return String.format("%.1f x %.1f x %.1f %s", length, width, height, unit.getDisplayName());
    }
}
