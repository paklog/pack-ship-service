package com.paklog.wes.pack.domain.valueobject;

/**
 * Supported shipping carriers
 */
public enum CarrierType {
    UPS("United Parcel Service", "1Z"),
    FEDEX("Federal Express", "FX"),
    USPS("United States Postal Service", "94"),
    DHL("DHL Express", "DH"),
    AMAZON_LOGISTICS("Amazon Logistics", "TBA"),
    CUSTOM("Custom Carrier", "CUSTOM");

    private final String fullName;
    private final String trackingPrefix;

    CarrierType(String fullName, String trackingPrefix) {
        this.fullName = fullName;
        this.trackingPrefix = trackingPrefix;
    }

    public String getFullName() {
        return fullName;
    }

    public String getTrackingPrefix() {
        return trackingPrefix;
    }

    public boolean supportsInternational() {
        return this == UPS || this == FEDEX || this == DHL;
    }

    public boolean supportsFreight() {
        return this == UPS || this == FEDEX;
    }

    public static CarrierType fromTrackingNumber(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isEmpty()) {
            return CUSTOM;
        }

        for (CarrierType carrier : values()) {
            if (trackingNumber.startsWith(carrier.trackingPrefix)) {
                return carrier;
            }
        }
        return CUSTOM;
    }
}
