package com.paklog.wes.pack.domain.aggregate;

import com.paklog.domain.annotation.AggregateRoot;
import com.paklog.domain.shared.DomainEvent;
import com.paklog.wes.pack.domain.entity.ShippingLabel;
import com.paklog.wes.pack.domain.event.ShipmentCreatedEvent;
import com.paklog.wes.pack.domain.event.ShipmentDispatchedEvent;
import com.paklog.wes.pack.domain.event.ShippingLabelGeneratedEvent;
import com.paklog.wes.pack.domain.valueobject.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Shipment aggregate root
 * Manages shipping and carrier operations
 */
@AggregateRoot
@Document(collection = "shipments")
public class Shipment {

    @Id
    private String shipmentId;

    @Version
    private Long version;

    private String packingSessionId;
    private String orderId;
    private String warehouseId;
    private CarrierType carrier;
    private ShippingMethod shippingMethod;
    private TrackingStatus trackingStatus;
    private String trackingNumber;
    private Address shippingAddress;
    private Weight weight;
    private Dimensions dimensions;
    private ShippingLabel shippingLabel;
    private LocalDateTime createdAt;
    private LocalDateTime labeledAt;
    private LocalDateTime manifestedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime estimatedDeliveryDate;
    private String manifestId;
    private String notes;

    private List<DomainEvent> domainEvents = new ArrayList<>();

    public Shipment() {
        // For MongoDB/persistence
    }

    /**
     * Create a new shipment
     */
    public static Shipment create(
            String packingSessionId,
            String orderId,
            String warehouseId,
            Address shippingAddress,
            CarrierType carrier,
            ShippingMethod shippingMethod,
            Weight weight,
            Dimensions dimensions
    ) {
        Shipment shipment = new Shipment();
        shipment.shipmentId = generateShipmentId();
        shipment.packingSessionId = Objects.requireNonNull(packingSessionId, "Packing session ID cannot be null");
        shipment.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        shipment.warehouseId = Objects.requireNonNull(warehouseId, "Warehouse ID cannot be null");
        shipment.shippingAddress = Objects.requireNonNull(shippingAddress, "Shipping address cannot be null");
        shipment.carrier = Objects.requireNonNull(carrier, "Carrier cannot be null");
        shipment.shippingMethod = Objects.requireNonNull(shippingMethod, "Shipping method cannot be null");
        shipment.weight = Objects.requireNonNull(weight, "Weight cannot be null");
        shipment.dimensions = dimensions;
        shipment.trackingStatus = TrackingStatus.CREATED;
        shipment.createdAt = LocalDateTime.now();

        // Validate address
        if (!shippingAddress.isValid()) {
            throw new IllegalArgumentException("Invalid shipping address");
        }

        // Calculate estimated delivery
        shipment.estimatedDeliveryDate = LocalDateTime.now()
                .plus(shippingMethod.getEstimatedDeliveryTime());

        shipment.registerEvent(new ShipmentCreatedEvent(
                shipment.shipmentId,
                packingSessionId,
                orderId,
                carrier,
                shippingMethod
        ));

        return shipment;
    }

    /**
     * Generate shipping label
     */
    public void generateLabel(ShippingLabel label, String trackingNumber) {
        ensureStatus(TrackingStatus.CREATED);

        Objects.requireNonNull(label, "Label cannot be null");
        Objects.requireNonNull(trackingNumber, "Tracking number cannot be null");

        if (!label.isValid()) {
            throw new IllegalArgumentException("Invalid shipping label");
        }

        this.shippingLabel = label;
        this.trackingNumber = trackingNumber;
        this.trackingStatus = TrackingStatus.LABELED;
        this.labeledAt = LocalDateTime.now();

        registerEvent(new ShippingLabelGeneratedEvent(
                this.shipmentId,
                trackingNumber,
                this.carrier,
                label.getFormat().name()
        ));
    }

    /**
     * Add to carrier manifest
     */
    public void addToManifest(String manifestId) {
        ensureStatus(TrackingStatus.LABELED);

        this.manifestId = Objects.requireNonNull(manifestId, "Manifest ID cannot be null");
        this.trackingStatus = TrackingStatus.MANIFESTED;
        this.manifestedAt = LocalDateTime.now();
    }

    /**
     * Mark shipment as dispatched
     */
    public void dispatch() {
        ensureStatus(TrackingStatus.MANIFESTED, TrackingStatus.LABELED);

        this.trackingStatus = TrackingStatus.PICKED_UP;
        this.shippedAt = LocalDateTime.now();

        registerEvent(new ShipmentDispatchedEvent(
                this.shipmentId,
                this.trackingNumber,
                this.carrier,
                this.shippedAt,
                this.warehouseId
        ));
    }

    /**
     * Update tracking status
     */
    public void updateTrackingStatus(TrackingStatus newStatus) {
        if (!this.trackingStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s",
                            this.trackingStatus, newStatus)
            );
        }

        this.trackingStatus = newStatus;

        // Set delivered date if delivered
        if (newStatus == TrackingStatus.DELIVERED) {
            this.deliveredAt = LocalDateTime.now();
        }
    }

    /**
     * Calculate shipping cost estimate
     */
    public double estimateShippingCost() {
        return shippingMethod.estimateShippingCost(weight.toPounds());
    }

    /**
     * Check if shipment is international
     */
    public boolean isInternational() {
        return shippingAddress.isInternational();
    }

    /**
     * Check if delivery is late
     */
    public boolean isLate() {
        if (deliveredAt != null) {
            return deliveredAt.isAfter(estimatedDeliveryDate);
        }
        // Still in transit - check if past estimated delivery
        return LocalDateTime.now().isAfter(estimatedDeliveryDate) &&
               !trackingStatus.isTerminal();
    }

    // Private helper methods

    private void ensureStatus(TrackingStatus... allowedStatuses) {
        for (TrackingStatus allowed : allowedStatuses) {
            if (this.trackingStatus == allowed) {
                return;
            }
        }
        throw new IllegalStateException(
                String.format("Invalid status. Current: %s, Expected: %s",
                        this.trackingStatus, List.of(allowedStatuses))
        );
    }

    private void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    private static String generateShipmentId() {
        return "SHIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters and setters

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getPackingSessionId() {
        return packingSessionId;
    }

    public void setPackingSessionId(String packingSessionId) {
        this.packingSessionId = packingSessionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public CarrierType getCarrier() {
        return carrier;
    }

    public void setCarrier(CarrierType carrier) {
        this.carrier = carrier;
    }

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(ShippingMethod shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public TrackingStatus getTrackingStatus() {
        return trackingStatus;
    }

    public void setTrackingStatus(TrackingStatus trackingStatus) {
        this.trackingStatus = trackingStatus;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Weight getWeight() {
        return weight;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = dimensions;
    }

    public ShippingLabel getShippingLabel() {
        return shippingLabel;
    }

    public void setShippingLabel(ShippingLabel shippingLabel) {
        this.shippingLabel = shippingLabel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLabeledAt() {
        return labeledAt;
    }

    public void setLabeledAt(LocalDateTime labeledAt) {
        this.labeledAt = labeledAt;
    }

    public LocalDateTime getManifestedAt() {
        return manifestedAt;
    }

    public void setManifestedAt(LocalDateTime manifestedAt) {
        this.manifestedAt = manifestedAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public String getManifestId() {
        return manifestId;
    }

    public void setManifestId(String manifestId) {
        this.manifestId = manifestId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<DomainEvent> getDomainEvents() {
        return domainEvents;
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shipment shipment = (Shipment) o;
        return Objects.equals(shipmentId, shipment.shipmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shipmentId);
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "shipmentId='" + shipmentId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", carrier=" + carrier +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", trackingStatus=" + trackingStatus +
                '}';
    }
}
