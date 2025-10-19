package com.paklog.wes.pack.domain.exception;

/**
 * Exception thrown when an item is scanned more than expected
 */
public class AlreadyScannedException extends RuntimeException {

    private final String barcode;
    private final int scannedQuantity;
    private final int expectedQuantity;

    public AlreadyScannedException(String barcode, int scannedQuantity, int expectedQuantity) {
        super(String.format("Item already fully scanned: %s. Scanned: %d, Expected: %d",
                barcode, scannedQuantity, expectedQuantity));
        this.barcode = barcode;
        this.scannedQuantity = scannedQuantity;
        this.expectedQuantity = expectedQuantity;
    }

    public String getBarcode() {
        return barcode;
    }

    public int getScannedQuantity() {
        return scannedQuantity;
    }

    public int getExpectedQuantity() {
        return expectedQuantity;
    }
}
