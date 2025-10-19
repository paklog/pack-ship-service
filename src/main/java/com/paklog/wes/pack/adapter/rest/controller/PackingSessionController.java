package com.paklog.wes.pack.adapter.rest.controller;

import com.paklog.wes.pack.adapter.rest.dto.*;
import com.paklog.wes.pack.application.command.*;
import com.paklog.wes.pack.application.service.PackingSessionService;
import com.paklog.wes.pack.domain.aggregate.PackingSession;
import com.paklog.wes.pack.domain.entity.QualityCheck;
import com.paklog.wes.pack.domain.valueobject.Weight;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for packing session operations
 */
@RestController
@RequestMapping("/api/v1/packing")
public class PackingSessionController {

    private static final Logger logger = LoggerFactory.getLogger(PackingSessionController.class);

    private final PackingSessionService packingSessionService;

    public PackingSessionController(PackingSessionService packingSessionService) {
        this.packingSessionService = packingSessionService;
    }

    /**
     * Start new packing session
     */
    @PostMapping("/sessions")
    public ResponseEntity<PackingSessionResponse> startSession(
            @Valid @RequestBody StartPackingRequest request
    ) {
        logger.info("Starting packing session for order: {}", request.orderId());

        StartPackingSessionCommand command = new StartPackingSessionCommand(
                request.pickSessionId(),
                request.orderId(),
                request.workerId(),
                request.warehouseId(),
                request.instructions()
        );

        PackingSession session = packingSessionService.createSession(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PackingSessionResponse.from(session));
    }

    /**
     * Get packing session by ID
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<PackingSessionResponse> getSession(@PathVariable String id) {
        logger.debug("Getting packing session: {}", id);

        PackingSession session = packingSessionService.getSession(id);
        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }

    /**
     * Pack item into container
     */
    @PostMapping("/sessions/{id}/pack")
    public ResponseEntity<PackingSessionResponse> packItem(
            @PathVariable String id,
            @Valid @RequestBody PackItemRequest request
    ) {
        logger.info("Packing item {} in session {}", request.instructionId(), id);

        PackItemCommand command = new PackItemCommand(
                id,
                request.instructionId(),
                request.containerId(),
                request.quantity()
        );

        PackingSession session = packingSessionService.packItem(command);
        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }

    /**
     * Seal container
     */
    @PostMapping("/sessions/{id}/seal/{containerId}")
    public ResponseEntity<PackingSessionResponse> sealContainer(
            @PathVariable String id,
            @PathVariable String containerId,
            @RequestParam double weightLb
    ) {
        logger.info("Sealing container {} in session {}", containerId, id);

        SealContainerCommand command = new SealContainerCommand(
                id,
                containerId,
                new Weight(weightLb, Weight.WeightUnit.LB)
        );

        PackingSession session = packingSessionService.sealContainer(command);
        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }

    /**
     * Complete packing session
     */
    @PostMapping("/sessions/{id}/complete")
    public ResponseEntity<PackingSessionResponse> completeSession(@PathVariable String id) {
        logger.info("Completing packing session: {}", id);

        PackingSession session = packingSessionService.completeSession(id);
        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }

    /**
     * Cancel packing session
     */
    @PostMapping("/sessions/{id}/cancel")
    public ResponseEntity<PackingSessionResponse> cancelSession(
            @PathVariable String id,
            @RequestParam String reason
    ) {
        logger.info("Cancelling packing session: {}", id);

        PackingSession session = packingSessionService.cancelSession(id, reason);
        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }

    /**
     * Get session progress
     */
    @GetMapping("/sessions/{id}/progress")
    public ResponseEntity<Double> getProgress(@PathVariable String id) {
        double progress = packingSessionService.getSessionProgress(id);
        return ResponseEntity.ok(progress);
    }

    /**
     * Get active sessions in warehouse
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<List<PackingSessionResponse>> getActiveSessions(
            @RequestParam String warehouseId
    ) {
        logger.debug("Getting active sessions for warehouse: {}", warehouseId);

        List<PackingSession> sessions = packingSessionService.getActiveSessionsByWarehouse(warehouseId);
        List<PackingSessionResponse> responses = sessions.stream()
                .map(PackingSessionResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // ========== Sprint 1: Scanning Workflow ==========

    /**
     * Initialize scanning workflow
     */
    @PostMapping("/sessions/{id}/scan/init")
    public ResponseEntity<PackingSessionResponse> initializeScanning(@PathVariable String id) {
        logger.info("Initializing scanning for session: {}", id);

        PackingSession session = packingSessionService.initializeScanning(id);
        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }

    /**
     * Scan an item
     */
    @PostMapping("/sessions/{id}/scan")
    public ResponseEntity<PackingSessionResponse> scanItem(
            @PathVariable String id,
            @Valid @RequestBody ScanItemRequest request
    ) {
        logger.info("Scanning item {} in session {}", request.barcode(), id);

        ScanItemCommand command = new ScanItemCommand(id, request.barcode());
        PackingSession session = packingSessionService.scanItem(command);

        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }

    // ========== Sprint 1: Carton Selection ==========

    /**
     * Get recommended carton
     */
    @GetMapping("/sessions/{id}/carton/recommend")
    public ResponseEntity<String> recommendCarton(@PathVariable String id) {
        logger.debug("Recommending carton for session: {}", id);

        String recommendedCarton = packingSessionService.recommendCarton(id);
        return ResponseEntity.ok(recommendedCarton);
    }

    /**
     * Select carton
     */
    @PostMapping("/sessions/{id}/carton")
    public ResponseEntity<PackingSessionResponse> selectCarton(
            @PathVariable String id,
            @Valid @RequestBody SelectCartonRequest request
    ) {
        logger.info("Selecting carton {} for session {}", request.cartonType(), id);

        SelectCartonCommand command = new SelectCartonCommand(id, request.cartonType());
        PackingSession session = packingSessionService.selectCarton(command);

        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }

    // ========== Sprint 1: Weight Verification ==========

    /**
     * Weigh and close package
     */
    @PostMapping("/sessions/{id}/weigh")
    public ResponseEntity<PackingSessionResponse> weighPackage(
            @PathVariable String id,
            @Valid @RequestBody WeighPackageRequest request
    ) {
        logger.info("Weighing package in session {}: {} lb", id, request.weightLb());

        WeighPackageCommand command = new WeighPackageCommand(
                id,
                new Weight(request.weightLb(), Weight.WeightUnit.LB)
        );
        PackingSession session = packingSessionService.weighPackage(command);

        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }

    // ========== Sprint 1: Quality Control ==========

    /**
     * Perform quality check
     */
    @PostMapping("/sessions/{id}/quality-check")
    public ResponseEntity<PackingSessionResponse> performQualityCheck(
            @PathVariable String id,
            @Valid @RequestBody QualityCheckRequest request
    ) {
        logger.info("Performing quality check for session {}", id);

        // Convert DTO checkpoints to domain checkpoints
        List<QualityCheck.Checkpoint> checkpoints = request.checkpoints().stream()
                .map(cp -> new QualityCheck.Checkpoint(cp.name(), cp.passed(), cp.notes()))
                .collect(Collectors.toList());

        PerformQualityCheckCommand command = new PerformQualityCheckCommand(
                id,
                request.checkerId(),
                checkpoints,
                request.photoUrls()
        );

        PackingSession session = packingSessionService.performQualityCheck(command);

        return ResponseEntity.ok(PackingSessionResponse.from(session));
    }
}
