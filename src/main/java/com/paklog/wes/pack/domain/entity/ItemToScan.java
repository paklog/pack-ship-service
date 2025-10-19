package com.paklog.wes.pack.domain.entity;

import java.util.Objects;

/**
 * Item expected to be scanned during packing
 */
public class ItemToScan {

    private String itemSku;
    private String barcode;
    private int expectedQuantity;
    private int scannedQuantity;
    private boolean scanned;

    public ItemToScan() {
        // For persistence
    }

    public ItemToScan(String itemSku, String barcode, int expectedQuantity) {
        this.itemSku = Objects.requireNonNull(itemSku, "SKU cannot be null");
        this.barcode = Objects.requireNonNull(barcode, "Barcode cannot be null");
        this.expectedQuantity = expectedQuantity;
        this.scannedQuantity = 0;
        this.scanned = false;
    }

    /**
     * Mark item as scanned
     */
    public void markScanned() {
        this.scannedQuantity++;
        if (this.scannedQuantity >= this.expectedQuantity) {
            this.scanned = true;
        }
    }

    /**
     * Check if all expected quantity is scanned
     */
    public boolean isFullyScanned() {
        return this.scannedQuantity >= this.expectedQuantity;
    }

    /**
     * Check if any quantity is scanned
     */
    public boolean isScanned() {
        return this.scanned || this.scannedQuantity > 0;
    }

    /**
     * Get remaining quantity to scan
     */
    public int getRemainingQuantity() {
        return Math.max(0, this.expectedQuantity - this.scannedQuantity);
    }

    // Getters and setters

    public String getItemSku() {
        return itemSku;
    }

    public void setItemSku(String itemSku) {
        this.itemSku = itemSku;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(int expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public int getScannedQuantity() {
        return scannedQuantity;
    }

    public void setScannedQuantity(int scannedQuantity) {
        this.scannedQuantity = scannedQuantity;
    }

    public boolean getScanned() {
        return scanned;
    }

    public void setScanned(boolean scanned) {
        this.scanned = scanned;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemToScan that = (ItemToScan) o;
        return Objects.equals(barcode, that.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode);
    }

    @Override
    public String toString() {
        return "ItemToScan{" +
                "itemSku='" + itemSku + '\'' +
                ", barcode='" + barcode + '\'' +
                ", scannedQuantity=" + scannedQuantity +
                '/' + expectedQuantity +
                ", scanned=" + scanned +
                '}';
    }
}
