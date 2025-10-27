package com.paklog.wes.pack.domain.valueobject;

public enum Priority {
    URGENT,   // Expedited shipment
    HIGH,     // High priority
    NORMAL,   // Standard
    LOW;      // Low priority

    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return Priority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
