package com.paklog.wes.pack.domain.valueobject;

import java.time.Duration;

/**
 * Shipping methods with delivery timeframes
 */
public enum ShippingMethod {
    SAME_DAY(Duration.ofHours(8), "Same Day", true, 50.0),
    NEXT_DAY(Duration.ofDays(1), "Next Day", true, 25.0),
    TWO_DAY(Duration.ofDays(2), "Two Day", false, 15.0),
    GROUND(Duration.ofDays(5), "Ground", false, 8.0),
    FREIGHT(Duration.ofDays(7), "Freight", false, 5.0),
    INTERNATIONAL(Duration.ofDays(14), "International", false, 30.0);

    private final Duration estimatedDeliveryTime;
    private final String displayName;
    private final boolean expedited;
    private final double baseRatePerLb;

    ShippingMethod(Duration estimatedDeliveryTime, String displayName, boolean expedited, double baseRatePerLb) {
        this.estimatedDeliveryTime = estimatedDeliveryTime;
        this.displayName = displayName;
        this.expedited = expedited;
        this.baseRatePerLb = baseRatePerLb;
    }

    public Duration getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isExpedited() {
        return expedited;
    }

    public double getBaseRatePerLb() {
        return baseRatePerLb;
    }

    public double estimateShippingCost(double weightLb) {
        return weightLb * baseRatePerLb;
    }
}
