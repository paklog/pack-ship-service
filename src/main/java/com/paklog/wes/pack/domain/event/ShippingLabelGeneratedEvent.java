package com.paklog.wes.pack.domain.event;

import com.paklog.wes.pack.domain.shared.DomainEvent;
import com.paklog.wes.pack.domain.valueobject.CarrierType;

public class ShippingLabelGeneratedEvent extends DomainEvent {
    private final String shipmentId;
    private final String trackingNumber;
    private final CarrierType carrier;
    private final String labelFormat;

    public ShippingLabelGeneratedEvent(String shipmentId, String trackingNumber,
                                      CarrierType carrier, String labelFormat) {
        super();
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.labelFormat = labelFormat;
    }

    public String getShipmentId() { return shipmentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public CarrierType getCarrier() { return carrier; }
    public String getLabelFormat() { return labelFormat; }
}
