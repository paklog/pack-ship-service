package com.paklog.wes.pack.domain.exception;

import com.paklog.wes.pack.domain.valueobject.Weight;

/**
 * Exception thrown when actual weight differs from estimated weight beyond tolerance
 */
public class WeightDiscrepancyException extends RuntimeException {

    private final Weight estimatedWeight;
    private final Weight actualWeight;
    private final double discrepancyPercentage;

    public WeightDiscrepancyException(Weight estimatedWeight, Weight actualWeight, double discrepancyPercentage) {
        super(String.format("Weight discrepancy detected. Estimated: %s, Actual: %s, Discrepancy: %.2f%%",
                estimatedWeight.toDisplayString(),
                actualWeight.toDisplayString(),
                discrepancyPercentage * 100));
        this.estimatedWeight = estimatedWeight;
        this.actualWeight = actualWeight;
        this.discrepancyPercentage = discrepancyPercentage;
    }

    public Weight getEstimatedWeight() {
        return estimatedWeight;
    }

    public Weight getActualWeight() {
        return actualWeight;
    }

    public double getDiscrepancyPercentage() {
        return discrepancyPercentage;
    }
}
