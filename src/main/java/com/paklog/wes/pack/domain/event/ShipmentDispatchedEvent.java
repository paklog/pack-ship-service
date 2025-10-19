package com.paklog.wes.pack.domain.event;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.wes.pack.domain.valueobject.CarrierType;
import java.time.LocalDateTime;

public class ShipmentDispatchedEvent extends DomainEvent {
    private final String shipmentId;
    private final String trackingNumber;
    private final CarrierType carrier;
    private final LocalDateTime dispatchTime;
    private final String warehouseId;

    public ShipmentDispatchedEvent(String shipmentId, String trackingNumber, CarrierType carrier,
                                  LocalDateTime dispatchTime, String warehouseId) {
        super();
        this.shipmentId = shipmentId;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.dispatchTime = dispatchTime;
        this.warehouseId = warehouseId;
    }

    public String getShipmentId() { return shipmentId; }
    public String getTrackingNumber() { return trackingNumber; }
    public CarrierType getCarrier() { return carrier; }
    public LocalDateTime getDispatchTime() { return dispatchTime; }
    public String getWarehouseId() { return warehouseId; }
}
