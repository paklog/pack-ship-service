package com.paklog.wes.pack.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/**
 * MongoDB configuration and index creation
 */
@Configuration
public class MongoConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        logger.info("Creating MongoDB indexes for Pack & Ship Service");

        createPackingSessionIndexes();
        createShipmentIndexes();

        logger.info("MongoDB indexes created successfully");
    }

    private void createPackingSessionIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("packing_sessions");

        // 1. Index on pickSessionId
        indexOps.ensureIndex(new Index().on("pickSessionId", Sort.Direction.ASC).named("idx_pick_session_id"));

        // 2. Index on orderId
        indexOps.ensureIndex(new Index().on("orderId", Sort.Direction.ASC).named("idx_order_id"));

        // 3. Compound index on workerId and status
        indexOps.ensureIndex(new Index()
                .on("workerId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_worker_status"));

        // 4. Compound index on warehouseId and status
        indexOps.ensureIndex(new Index()
                .on("warehouseId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_warehouse_status"));

        // 5. Index on status
        indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC).named("idx_status"));

        // 6. Index on createdAt
        indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).named("idx_created_at"));

        // 7. Index on completedAt
        indexOps.ensureIndex(new Index().on("completedAt", Sort.Direction.DESC).named("idx_completed_at"));

        logger.debug("Created 7 indexes for packing_sessions collection");
    }

    private void createShipmentIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("shipments");

        // 1. Index on trackingNumber (unique)
        indexOps.ensureIndex(new Index().on("trackingNumber", Sort.Direction.ASC)
                .unique()
                .named("idx_tracking_number"));

        // 2. Index on packingSessionId
        indexOps.ensureIndex(new Index().on("packingSessionId", Sort.Direction.ASC).named("idx_packing_session_id"));

        // 3. Index on orderId
        indexOps.ensureIndex(new Index().on("orderId", Sort.Direction.ASC).named("idx_shipment_order_id"));

        // 4. Compound index on carrier and trackingStatus
        indexOps.ensureIndex(new Index()
                .on("carrier", Sort.Direction.ASC)
                .on("trackingStatus", Sort.Direction.ASC)
                .named("idx_carrier_status"));

        // 5. Index on manifestId
        indexOps.ensureIndex(new Index().on("manifestId", Sort.Direction.ASC).named("idx_manifest_id"));

        // 6. Index on createdAt
        indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).named("idx_shipment_created_at"));

        // 7. Compound index on warehouseId and trackingStatus
        indexOps.ensureIndex(new Index()
                .on("warehouseId", Sort.Direction.ASC)
                .on("trackingStatus", Sort.Direction.ASC)
                .named("idx_warehouse_tracking_status"));

        logger.debug("Created 7 indexes for shipments collection");
    }
}
