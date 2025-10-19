package com.paklog.wes.pack.domain.entity;

import com.paklog.wes.pack.domain.valueobject.CarrierType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Shipping label entity
 */
public class ShippingLabel {

    public enum LabelFormat {
        PDF,
        ZPL,  // Zebra Programming Language
        PNG,
        EPL   // Eltron Programming Language
    }

    private String labelId;
    private String trackingNumber;
    private CarrierType carrier;
    private LabelFormat format;
    private String labelData; // Base64 encoded or raw format data
    private String barcode;    // Barcode data
    private LocalDateTime generatedAt;
    private String generatedBy;
    private boolean printed;
    private LocalDateTime printedAt;

    public ShippingLabel() {
        // For persistence
    }

    public ShippingLabel(
            String trackingNumber,
            CarrierType carrier,
            LabelFormat format,
            String labelData,
            String barcode
    ) {
        this.labelId = generateLabelId(trackingNumber);
        this.trackingNumber = Objects.requireNonNull(trackingNumber, "Tracking number cannot be null");
        this.carrier = Objects.requireNonNull(carrier, "Carrier cannot be null");
        this.format = Objects.requireNonNull(format, "Format cannot be null");
        this.labelData = Objects.requireNonNull(labelData, "Label data cannot be null");
        this.barcode = barcode;
        this.generatedAt = LocalDateTime.now();
        this.printed = false;
    }

    /**
     * Mark label as printed
     */
    public void markPrinted() {
        this.printed = true;
        this.printedAt = LocalDateTime.now();
    }

    /**
     * Validate label data
     */
    public boolean isValid() {
        return trackingNumber != null && !trackingNumber.isBlank() &&
               carrier != null &&
               format != null &&
               labelData != null && !labelData.isBlank();
    }

    /**
     * Get file extension for label format
     */
    public String getFileExtension() {
        return switch (format) {
            case PDF -> ".pdf";
            case ZPL -> ".zpl";
            case PNG -> ".png";
            case EPL -> ".epl";
        };
    }

    /**
     * Get MIME type for label format
     */
    public String getMimeType() {
        return switch (format) {
            case PDF -> "application/pdf";
            case ZPL -> "text/plain";
            case PNG -> "image/png";
            case EPL -> "text/plain";
        };
    }

    private String generateLabelId(String trackingNumber) {
        return "LABEL-" + trackingNumber;
    }

    // Getters and setters

    public String getLabelId() {
        return labelId;
    }

    public void setLabelId(String labelId) {
        this.labelId = labelId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public CarrierType getCarrier() {
        return carrier;
    }

    public void setCarrier(CarrierType carrier) {
        this.carrier = carrier;
    }

    public LabelFormat getFormat() {
        return format;
    }

    public void setFormat(LabelFormat format) {
        this.format = format;
    }

    public String getLabelData() {
        return labelData;
    }

    public void setLabelData(String labelData) {
        this.labelData = labelData;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public boolean isPrinted() {
        return printed;
    }

    public void setPrinted(boolean printed) {
        this.printed = printed;
    }

    public LocalDateTime getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(LocalDateTime printedAt) {
        this.printedAt = printedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShippingLabel that = (ShippingLabel) o;
        return Objects.equals(labelId, that.labelId) &&
               Objects.equals(trackingNumber, that.trackingNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelId, trackingNumber);
    }

    @Override
    public String toString() {
        return "ShippingLabel{" +
                "trackingNumber='" + trackingNumber + '\'' +
                ", carrier=" + carrier +
                ", format=" + format +
                ", printed=" + printed +
                '}';
    }
}
