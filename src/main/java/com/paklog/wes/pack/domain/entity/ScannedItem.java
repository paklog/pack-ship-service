package com.paklog.wes.pack.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Record of a scanned item with timestamp
 */
public class ScannedItem {

    private String itemSku;
    private String barcode;
    private LocalDateTime scannedAt;
    private String scannedBy;

    public ScannedItem() {
        // For persistence
    }

    public ScannedItem(ItemToScan item, LocalDateTime scannedAt) {
        this.itemSku = item.getItemSku();
        this.barcode = item.getBarcode();
        this.scannedAt = scannedAt;
    }

    public ScannedItem(ItemToScan item, LocalDateTime scannedAt, String scannedBy) {
        this.itemSku = item.getItemSku();
        this.barcode = item.getBarcode();
        this.scannedAt = scannedAt;
        this.scannedBy = scannedBy;
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

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }

    public String getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScannedItem that = (ScannedItem) o;
        return Objects.equals(barcode, that.barcode) &&
               Objects.equals(scannedAt, that.scannedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode, scannedAt);
    }

    @Override
    public String toString() {
        return "ScannedItem{" +
                "itemSku='" + itemSku + '\'' +
                ", barcode='" + barcode + '\'' +
                ", scannedAt=" + scannedAt +
                '}';
    }
}
