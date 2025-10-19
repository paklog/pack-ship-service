package com.paklog.wes.pack.domain.exception;

import com.paklog.wes.pack.domain.valueobject.ContainerType;

/**
 * Exception thrown when selected carton is not suitable for items
 */
public class UnsuitableCartonException extends RuntimeException {

    private final ContainerType selectedCarton;
    private final String reason;

    public UnsuitableCartonException(ContainerType selectedCarton, String reason) {
        super(String.format("Selected carton %s is not suitable: %s", selectedCarton, reason));
        this.selectedCarton = selectedCarton;
        this.reason = reason;
    }

    public ContainerType getSelectedCarton() {
        return selectedCarton;
    }

    public String getReason() {
        return reason;
    }
}
