package com.paklog.wes.pack.domain.repository;

import com.paklog.wes.pack.domain.aggregate.PackingSession;
import com.paklog.wes.pack.domain.valueobject.PackingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PackingSession aggregate
 */
@Repository
public interface PackingSessionRepository extends MongoRepository<PackingSession, String> {

    /**
     * Find packing session by pick session ID
     */
    Optional<PackingSession> findByPickSessionId(String pickSessionId);

    /**
     * Find packing session by order ID
     */
    Optional<PackingSession> findByOrderId(String orderId);

    /**
     * Find active packing session for worker
     */
    @Query("{'workerId': ?0, 'status': {$in: ['CREATED', 'IN_PROGRESS', 'QUALITY_CHECK']}}")
    Optional<PackingSession> findActiveSessionByWorkerId(String workerId);

    /**
     * Find packing sessions by worker and status
     */
    List<PackingSession> findByWorkerIdAndStatus(String workerId, PackingStatus status);

    /**
     * Find packing sessions by warehouse and status
     */
    List<PackingSession> findByWarehouseIdAndStatus(String warehouseId, PackingStatus status);

    /**
     * Find all packing sessions by status
     */
    List<PackingSession> findByStatus(PackingStatus status);

    /**
     * Find active packing sessions in warehouse
     */
    @Query("{'warehouseId': ?0, 'status': {$in: ['CREATED', 'IN_PROGRESS', 'QUALITY_CHECK']}}")
    List<PackingSession> findActiveSessionsByWarehouse(String warehouseId);

    /**
     * Find packing sessions created after date
     */
    List<PackingSession> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find packing sessions completed between dates
     */
    List<PackingSession> findByStatusAndCompletedAtBetween(
            PackingStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Count active packing sessions
     */
    @Query(value = "{'status': {$in: ['CREATED', 'IN_PROGRESS', 'QUALITY_CHECK']}}", count = true)
    long countActiveSessions();

    /**
     * Count packing sessions by worker and date range
     */
    long countByWorkerIdAndCreatedAtBetween(String workerId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find all packing sessions for worker
     */
    List<PackingSession> findByWorkerId(String workerId);
}
