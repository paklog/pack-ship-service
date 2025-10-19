package com.paklog.wes.pack.application.command;

import com.paklog.wes.pack.domain.valueobject.Address;
import com.paklog.wes.pack.domain.valueobject.CarrierType;
import com.paklog.wes.pack.domain.valueobject.ShippingMethod;

/**
 * Command to create a shipment from packing session
 */
public record CreateShipmentCommand(
        String packingSessionId,
        Address shippingAddress,
        CarrierType carrier,
        ShippingMethod shippingMethod
) {
}
