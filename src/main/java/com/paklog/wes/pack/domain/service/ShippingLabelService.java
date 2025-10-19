package com.paklog.wes.pack.domain.service;

import com.paklog.wes.pack.domain.entity.ShippingLabel;
import com.paklog.wes.pack.domain.valueobject.Address;
import com.paklog.wes.pack.domain.valueobject.CarrierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

/**
 * Domain service for shipping label generation
 * Provides mock label generation (extensible for real carrier APIs)
 */
@Service
public class ShippingLabelService {

    private static final Logger logger = LoggerFactory.getLogger(ShippingLabelService.class);

    /**
     * Generate tracking number for carrier
     */
    public String generateTrackingNumber(CarrierType carrier) {
        String prefix = carrier.getTrackingPrefix();
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        String trackingNumber = switch (carrier) {
            case UPS -> prefix + uniqueId + "01"; // UPS format: 1Z + 16 chars
            case FEDEX -> prefix + uniqueId;      // FedEx format: 12-14 digits
            case USPS -> prefix + uniqueId;       // USPS format: 20-22 digits
            case DHL -> prefix + uniqueId;        // DHL format: 10 digits
            case AMAZON_LOGISTICS -> prefix + uniqueId;
            case CUSTOM -> "CUSTOM-" + uniqueId;
        };

        logger.debug("Generated tracking number: {} for carrier: {}", trackingNumber, carrier);
        return trackingNumber;
    }

    /**
     * Generate mock shipping label
     * In production, this would call real carrier APIs
     */
    public ShippingLabel generateLabel(
            CarrierType carrier,
            String trackingNumber,
            Address shippingAddress,
            Address fromAddress,
            double weightLb
    ) {
        logger.info("Generating shipping label for tracking number: {}", trackingNumber);

        // Validate addresses
        if (!shippingAddress.isValid()) {
            throw new IllegalArgumentException("Invalid shipping address");
        }

        // Generate mock label data (in production, call carrier API)
        String labelData = generateMockLabelData(carrier, trackingNumber, shippingAddress, fromAddress, weightLb);

        // Generate barcode data
        String barcode = generateBarcodeData(trackingNumber);

        // Create shipping label
        ShippingLabel label = new ShippingLabel(
                trackingNumber,
                carrier,
                ShippingLabel.LabelFormat.PDF, // Default to PDF
                labelData,
                barcode
        );

        logger.info("Successfully generated shipping label for tracking: {}", trackingNumber);
        return label;
    }

    /**
     * Validate shipping address
     */
    public boolean validateAddress(Address address) {
        if (address == null) {
            return false;
        }

        // Basic validation
        if (!address.isValid()) {
            logger.warn("Address validation failed: incomplete address");
            return false;
        }

        // Validate zip code format (US only for now)
        if ("US".equalsIgnoreCase(address.country())) {
            String zip = address.zipCode();
            // US zip: 5 digits or 5+4 digits
            if (!zip.matches("\\d{5}") && !zip.matches("\\d{5}-\\d{4}")) {
                logger.warn("Invalid US zip code format: {}", zip);
                return false;
            }
        }

        logger.debug("Address validation successful for: {}", address.city());
        return true;
    }

    /**
     * Generate ZPL (Zebra Programming Language) format label
     */
    public String generateZPLLabel(String trackingNumber, Address shippingAddress) {
        // Mock ZPL format - in production, use real ZPL template
        StringBuilder zpl = new StringBuilder();
        zpl.append("^XA\n"); // Start of label
        zpl.append("^FO50,50^A0N,50,50^FD").append(trackingNumber).append("^FS\n"); // Tracking number
        zpl.append("^FO50,120^A0N,30,30^FD").append(shippingAddress.street1()).append("^FS\n");
        zpl.append("^FO50,160^A0N,30,30^FD").append(shippingAddress.city()).append(", ")
                .append(shippingAddress.state()).append(" ").append(shippingAddress.zipCode()).append("^FS\n");
        zpl.append("^FO50,250^BY3^BCN,100,Y,N,N\n"); // Barcode
        zpl.append("^FD").append(trackingNumber).append("^FS\n");
        zpl.append("^XZ\n"); // End of label

        return zpl.toString();
    }

    /**
     * Calculate shipping cost estimate
     */
    public double calculateShippingCost(CarrierType carrier, double weightLb, boolean isInternational) {
        // Mock calculation - in production, call carrier rating API
        double baseRate = switch (carrier) {
            case UPS -> 8.50;
            case FEDEX -> 9.00;
            case USPS -> 7.00;
            case DHL -> 12.00;
            case AMAZON_LOGISTICS -> 6.00;
            case CUSTOM -> 10.00;
        };

        double cost = baseRate + (weightLb * 0.50);

        // Add international surcharge
        if (isInternational) {
            cost *= 2.5;
        }

        logger.debug("Calculated shipping cost: ${} for carrier: {}, weight: {} lb, international: {}",
                String.format("%.2f", cost), carrier, weightLb, isInternational);

        return cost;
    }

    // Private helper methods

    private String generateMockLabelData(
            CarrierType carrier,
            String trackingNumber,
            Address shippingAddress,
            Address fromAddress,
            double weightLb
    ) {
        // In production, this would call carrier API and return actual label PDF/image
        // For now, generate mock base64 encoded data
        String labelContent = String.format(
                "MOCK SHIPPING LABEL\nCarrier: %s\nTracking: %s\n\nShip To:\n%s\n%s, %s %s\n%s\n\nWeight: %.2f lb",
                carrier.getFullName(),
                trackingNumber,
                shippingAddress.street1(),
                shippingAddress.city(),
                shippingAddress.state(),
                shippingAddress.zipCode(),
                shippingAddress.country(),
                weightLb
        );

        return Base64.getEncoder().encodeToString(labelContent.getBytes());
    }

    private String generateBarcodeData(String trackingNumber) {
        // Generate barcode data (in production, use actual barcode generation library)
        return Base64.getEncoder().encodeToString(trackingNumber.getBytes());
    }
}
