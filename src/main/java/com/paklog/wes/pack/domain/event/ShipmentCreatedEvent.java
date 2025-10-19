package com.paklog.wes.pack.domain.event;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.wes.pack.domain.valueobject.CarrierType;
import com.paklog.wes.pack.domain.valueobject.ShippingMethod;

public class ShipmentCreatedEvent extends DomainEvent {
    private final String shipmentId;
    private final String packingSessionId;
    private final String orderId;
    private final CarrierType carrier;
    private final ShippingMethod shippingMethod;

    public ShipmentCreatedEvent(String shipmentId, String packingSessionId, String orderId,
                               CarrierType carrier, ShippingMethod shippingMethod) {
        super();
        this.shipmentId = shipmentId;
        this.packingSessionId = packingSessionId;
        this.orderId = orderId;
        this.carrier = carrier;
        this.shippingMethod = shippingMethod;
    }

    public String getShipmentId() { return shipmentId; }
    public String getPackingSessionId() { return packingSessionId; }
    public String getOrderId() { return orderId; }
    public CarrierType getCarrier() { return carrier; }
    public ShippingMethod getShippingMethod() { return shippingMethod; }
}
