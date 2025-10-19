package com.paklog.wes.pack.domain.exception;

/**
 * Exception thrown when an unexpected item is scanned
 */
public class UnexpectedItemException extends RuntimeException {

    private final String barcode;

    public UnexpectedItemException(String barcode) {
        super("Unexpected item scanned: " + barcode + ". This item is not expected in this packing session.");
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}
