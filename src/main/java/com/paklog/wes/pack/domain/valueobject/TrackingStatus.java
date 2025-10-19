package com.paklog.wes.pack.domain.valueobject;

/**
 * Shipment tracking lifecycle states
 */
public enum TrackingStatus {
    CREATED("Label created, awaiting pickup"),
    LABELED("Shipping label generated"),
    MANIFESTED("Added to carrier manifest"),
    PICKED_UP("Picked up by carrier"),
    IN_TRANSIT("In transit to destination"),
    OUT_FOR_DELIVERY("Out for delivery"),
    DELIVERED("Successfully delivered"),
    EXCEPTION("Delivery exception occurred"),
    RETURNED("Returned to sender");

    private final String description;

    TrackingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED;
    }

    public boolean isActive() {
        return !isTerminal() && this != EXCEPTION;
    }

    public boolean canTransitionTo(TrackingStatus newStatus) {
        return switch (this) {
            case CREATED -> newStatus == LABELED || newStatus == EXCEPTION;
            case LABELED -> newStatus == MANIFESTED || newStatus == EXCEPTION;
            case MANIFESTED -> newStatus == PICKED_UP || newStatus == EXCEPTION;
            case PICKED_UP -> newStatus == IN_TRANSIT || newStatus == EXCEPTION;
            case IN_TRANSIT -> newStatus == OUT_FOR_DELIVERY || newStatus == EXCEPTION;
            case OUT_FOR_DELIVERY -> newStatus == DELIVERED || newStatus == EXCEPTION || newStatus == RETURNED;
            case EXCEPTION -> newStatus == IN_TRANSIT || newStatus == RETURNED;
            case DELIVERED, RETURNED -> false;
        };
    }
}
