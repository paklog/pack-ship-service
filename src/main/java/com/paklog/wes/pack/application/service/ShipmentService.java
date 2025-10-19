package com.paklog.wes.pack.application.service;

import com.paklog.wes.pack.application.command.CreateShipmentCommand;
import com.paklog.wes.pack.application.command.GenerateLabelCommand;
import com.paklog.wes.pack.domain.aggregate.PackingSession;
import com.paklog.wes.pack.domain.aggregate.Shipment;
import com.paklog.wes.pack.domain.entity.ShippingLabel;
import com.paklog.wes.pack.domain.repository.PackingSessionRepository;
import com.paklog.wes.pack.domain.repository.ShipmentRepository;
import com.paklog.wes.pack.domain.service.ShippingLabelService;
import com.paklog.wes.pack.domain.valueobject.Dimensions;
import com.paklog.wes.pack.domain.valueobject.TrackingStatus;
import com.paklog.wes.pack.domain.valueobject.Weight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for shipment operations
 */
@Service
public class ShipmentService {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;
    private final PackingSessionRepository packingSessionRepository;
    private final ShippingLabelService labelService;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            PackingSessionRepository packingSessionRepository,
            ShippingLabelService labelService
    ) {
        this.shipmentRepository = shipmentRepository;
        this.packingSessionRepository = packingSessionRepository;
        this.labelService = labelService;
    }

    /**
     * Create shipment from packing session
     */
    @Transactional
    public Shipment createShipment(CreateShipmentCommand command) {
        logger.info("Creating shipment for packing session: {}", command.packingSessionId());

        // Get packing session
        PackingSession session = packingSessionRepository.findById(command.packingSessionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Packing session not found: " + command.packingSessionId()));

        // Validate session is completed
        if (session.getStatus() != com.paklog.wes.pack.domain.valueobject.PackingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot create shipment for incomplete packing session");
        }

        // Validate address
        if (!labelService.validateAddress(command.shippingAddress())) {
            throw new IllegalArgumentException("Invalid shipping address");
        }

        // Calculate total weight and dimensions
        Weight totalWeight = new Weight(session.getTotalWeight(), Weight.WeightUnit.LB);
        Dimensions dimensions = estimateDimensions(session);

        // Create shipment
        Shipment shipment = Shipment.create(
                command.packingSessionId(),
                session.getOrderId(),
                session.getWarehouseId(),
                command.shippingAddress(),
                command.carrier(),
                command.shippingMethod(),
                totalWeight,
                dimensions
        );

        logger.info("Created shipment: {} for order: {}", shipment.getShipmentId(), session.getOrderId());

        return shipmentRepository.save(shipment);
    }

    /**
     * Generate shipping label
     */
    @Transactional
    public Shipment generateLabel(GenerateLabelCommand command) {
        logger.info("Generating label for shipment: {}", command.shipmentId());

        Shipment shipment = findShipmentById(command.shipmentId());

        // Generate tracking number
        String trackingNumber = labelService.generateTrackingNumber(shipment.getCarrier());

        // Generate label
        ShippingLabel label = labelService.generateLabel(
                shipment.getCarrier(),
                trackingNumber,
                shipment.getShippingAddress(),
                command.fromAddress(),
                shipment.getWeight().toPounds()
        );

        // Add label to shipment
        shipment.generateLabel(label, trackingNumber);

        logger.info("Generated shipping label with tracking number: {}", trackingNumber);

        return shipmentRepository.save(shipment);
    }

    /**
     * Add shipment to carrier manifest
     */
    @Transactional
    public Shipment addToManifest(String shipmentId) {
        logger.info("Adding shipment {} to manifest", shipmentId);

        Shipment shipment = findShipmentById(shipmentId);

        // Generate manifest ID (in production, this would come from carrier API)
        String manifestId = generateManifestId(shipment.getCarrier());

        shipment.addToManifest(manifestId);

        logger.info("Shipment {} added to manifest {}", shipmentId, manifestId);

        return shipmentRepository.save(shipment);
    }

    /**
     * Mark shipment as dispatched
     */
    @Transactional
    public Shipment dispatchShipment(String shipmentId) {
        logger.info("Dispatching shipment: {}", shipmentId);

        Shipment shipment = findShipmentById(shipmentId);
        shipment.dispatch();

        logger.info("Shipment {} dispatched successfully", shipmentId);

        return shipmentRepository.save(shipment);
    }

    /**
     * Update tracking status
     */
    @Transactional
    public Shipment updateTrackingStatus(String shipmentId, TrackingStatus newStatus) {
        logger.info("Updating tracking status for shipment {} to {}", shipmentId, newStatus);

        Shipment shipment = findShipmentById(shipmentId);
        shipment.updateTrackingStatus(newStatus);

        return shipmentRepository.save(shipment);
    }

    /**
     * Get shipment by ID
     */
    public Shipment getShipment(String shipmentId) {
        return findShipmentById(shipmentId);
    }

    /**
     * Get shipment by tracking number
     */
    public Shipment getShipmentByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Shipment not found for tracking number: " + trackingNumber));
    }

    /**
     * Get shipments by order ID
     */
    public List<Shipment> getShipmentsByOrder(String orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }

    /**
     * Get shipments ready for manifest
     */
    public List<Shipment> getShipmentsReadyForManifest(String carrier) {
        return shipmentRepository.findShipmentsReadyForManifest(
                com.paklog.wes.pack.domain.valueobject.CarrierType.valueOf(carrier)
        );
    }

    /**
     * Get in-transit shipments
     */
    public List<Shipment> getInTransitShipments() {
        return shipmentRepository.findInTransitShipments();
    }

    /**
     * Get late shipments
     */
    public List<Shipment> getLateShipments() {
        return shipmentRepository.findLateShipments(java.time.LocalDateTime.now());
    }

    // Private helper methods

    private Shipment findShipmentById(String shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));
    }

    private Dimensions estimateDimensions(PackingSession session) {
        // In production, calculate based on actual containers
        // For now, use standard box dimensions
        if (session.getContainers().isEmpty()) {
            return new Dimensions(12, 9, 6, Dimensions.DimensionUnit.IN);
        }

        // Use largest container dimensions
        return session.getContainers().stream()
                .map(c -> c.getDimensions())
                .max((d1, d2) -> Double.compare(
                        d1.getVolumeInCubicInches(),
                        d2.getVolumeInCubicInches()
                ))
                .orElse(new Dimensions(12, 9, 6, Dimensions.DimensionUnit.IN));
    }

    private String generateManifestId(com.paklog.wes.pack.domain.valueobject.CarrierType carrier) {
        // In production, get from carrier API
        return "MANIFEST-" + carrier.name() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
