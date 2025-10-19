package com.paklog.wes.pack.domain.valueobject;

/**
 * Packing session lifecycle states
 */
public enum PackingStatus {
    CREATED,
    SCANNING,
    READY_FOR_CARTON,
    READY_TO_PACK,
    PACKING,
    IN_PROGRESS,
    QC_REQUIRED,
    QC_PASSED,
    QC_FAILED,
    READY_TO_WEIGH,
    READY_TO_SHIP,
    COMPLETED,
    CANCELLED,
    FAILED;

    public boolean isActive() {
        return this == CREATED || this == SCANNING || this == READY_FOR_CARTON ||
               this == READY_TO_PACK || this == PACKING || this == IN_PROGRESS ||
               this == QC_REQUIRED || this == QC_PASSED || this == READY_TO_WEIGH;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED || this == READY_TO_SHIP;
    }

    public boolean canTransitionTo(PackingStatus newStatus) {
        return switch (this) {
            case CREATED -> newStatus == SCANNING || newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case SCANNING -> newStatus == READY_FOR_CARTON || newStatus == CANCELLED;
            case READY_FOR_CARTON -> newStatus == READY_TO_PACK || newStatus == CANCELLED;
            case READY_TO_PACK -> newStatus == PACKING || newStatus == CANCELLED;
            case PACKING -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == QC_REQUIRED || newStatus == READY_TO_WEIGH ||
                               newStatus == COMPLETED || newStatus == CANCELLED || newStatus == FAILED;
            case QC_REQUIRED -> newStatus == QC_PASSED || newStatus == QC_FAILED || newStatus == CANCELLED;
            case QC_PASSED -> newStatus == READY_TO_WEIGH || newStatus == COMPLETED;
            case QC_FAILED -> newStatus == PACKING || newStatus == CANCELLED;
            case READY_TO_WEIGH -> newStatus == READY_TO_SHIP || newStatus == CANCELLED;
            case READY_TO_SHIP -> newStatus == COMPLETED;
            case COMPLETED, CANCELLED, FAILED -> false;
        };
    }
}
