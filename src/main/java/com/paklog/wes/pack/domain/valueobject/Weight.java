package com.paklog.wes.pack.domain.valueobject;

import java.util.Objects;

/**
 * Weight value object with unit conversion
 */
public record Weight(double value, WeightUnit unit) {

    public enum WeightUnit {
        LB("Pounds"),
        KG("Kilograms"),
        OZ("Ounces"),
        G("Grams");

        private final String displayName;

        WeightUnit(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Weight {
        if (value < 0) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
        Objects.requireNonNull(unit, "Weight unit cannot be null");
    }

    public Weight(double value) {
        this(value, WeightUnit.LB);
    }

    public double toPounds() {
        return switch (unit) {
            case LB -> value;
            case KG -> value * 2.20462;
            case OZ -> value / 16.0;
            case G -> value * 0.00220462;
        };
    }

    public double toKilograms() {
        return switch (unit) {
            case LB -> value / 2.20462;
            case KG -> value;
            case OZ -> value / 35.274;
            case G -> value / 1000.0;
        };
    }

    public Weight convertTo(WeightUnit targetUnit) {
        if (unit == targetUnit) {
            return this;
        }

        double pounds = toPounds();
        double newValue = switch (targetUnit) {
            case LB -> pounds;
            case KG -> pounds / 2.20462;
            case OZ -> pounds * 16.0;
            case G -> pounds / 0.00220462;
        };

        return new Weight(newValue, targetUnit);
    }

    public Weight add(Weight other) {
        double totalPounds = this.toPounds() + other.toPounds();
        return new Weight(totalPounds, WeightUnit.LB).convertTo(this.unit);
    }

    public boolean isGreaterThan(Weight other) {
        return this.toPounds() > other.toPounds();
    }

    public String toDisplayString() {
        return String.format("%.2f %s", value, unit.getDisplayName());
    }
}
