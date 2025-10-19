package com.paklog.wes.pack.domain.repository;

import com.paklog.wes.pack.domain.aggregate.Shipment;
import com.paklog.wes.pack.domain.valueobject.CarrierType;
import com.paklog.wes.pack.domain.valueobject.TrackingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Shipment aggregate
 */
@Repository
public interface ShipmentRepository extends MongoRepository<Shipment, String> {

    /**
     * Find shipment by tracking number
     */
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    /**
     * Find shipment by packing session ID
     */
    Optional<Shipment> findByPackingSessionId(String packingSessionId);

    /**
     * Find shipments by order ID
     */
    List<Shipment> findByOrderId(String orderId);

    /**
     * Find shipments by warehouse and status
     */
    List<Shipment> findByWarehouseIdAndTrackingStatus(String warehouseId, TrackingStatus status);

    /**
     * Find shipments by carrier and status
     */
    List<Shipment> findByCarrierAndTrackingStatus(CarrierType carrier, TrackingStatus status);

    /**
     * Find shipments by manifest ID
     */
    List<Shipment> findByManifestId(String manifestId);

    /**
     * Find shipments created after date
     */
    List<Shipment> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find shipments shipped between dates
     */
    List<Shipment> findByShippedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find late shipments
     */
    @Query("{'estimatedDeliveryDate': {$lt: ?0}, 'trackingStatus': {$nin: ['DELIVERED', 'RETURNED']}}")
    List<Shipment> findLateShipments(LocalDateTime currentDate);

    /**
     * Find shipments ready for manifest
     */
    @Query("{'trackingStatus': 'LABELED', 'carrier': ?0}")
    List<Shipment> findShipmentsReadyForManifest(CarrierType carrier);

    /**
     * Find in-transit shipments
     */
    @Query("{'trackingStatus': {$in: ['PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY']}}")
    List<Shipment> findInTransitShipments();

    /**
     * Count shipments by carrier and date range
     */
    long countByCarrierAndCreatedAtBetween(
            CarrierType carrier,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find all shipments by tracking status
     */
    List<Shipment> findByTrackingStatus(TrackingStatus status);
}
