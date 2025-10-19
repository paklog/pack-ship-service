package com.paklog.wes.pack.application.service;

import com.paklog.wes.pack.application.command.*;
import com.paklog.wes.pack.application.command.PackItemCommand;
import com.paklog.wes.pack.application.command.SealContainerCommand;
import com.paklog.wes.pack.application.command.StartPackingSessionCommand;
import com.paklog.wes.pack.domain.aggregate.PackingSession;
import com.paklog.wes.pack.domain.entity.Container;
import com.paklog.wes.pack.domain.entity.PackingInstruction;
import com.paklog.wes.pack.domain.repository.PackingSessionRepository;
import com.paklog.wes.pack.domain.service.ContainerOptimizationService;
import com.paklog.wes.pack.domain.valueobject.ContainerType;
import com.paklog.wes.pack.domain.valueobject.PackingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service for packing session operations
 */
@Service
public class PackingSessionService {

    private static final Logger logger = LoggerFactory.getLogger(PackingSessionService.class);

    private final PackingSessionRepository sessionRepository;
    private final ContainerOptimizationService containerOptimizationService;

    public PackingSessionService(
            PackingSessionRepository sessionRepository,
            ContainerOptimizationService containerOptimizationService
    ) {
        this.sessionRepository = sessionRepository;
        this.containerOptimizationService = containerOptimizationService;
    }

    /**
     * Create and start a new packing session
     */
    @Transactional
    public PackingSession createSession(StartPackingSessionCommand command) {
        logger.info("Creating packing session for order: {}", command.orderId());

        // Check for existing active session for worker
        Optional<PackingSession> existingSession =
                sessionRepository.findActiveSessionByWorkerId(command.workerId());

        if (existingSession.isPresent()) {
            throw new IllegalStateException(
                    "Worker already has active packing session: " + existingSession.get().getSessionId()
            );
        }

        // Create session
        PackingSession session = PackingSession.create(
                command.pickSessionId(),
                command.orderId(),
                command.workerId(),
                command.warehouseId(),
                command.instructions()
        );

        // Start session
        session.start();

        // Recommend initial container
        ContainerType recommendedType = containerOptimizationService.recommendContainer(
                command.instructions()
        );

        // Create initial container
        Container initialContainer = Container.create(recommendedType);
        session.addContainer(initialContainer);

        logger.info("Created packing session: {} with initial container: {}",
                session.getSessionId(), initialContainer.getContainerId());

        return sessionRepository.save(session);
    }

    /**
     * Pack item into container
     */
    @Transactional
    public PackingSession packItem(PackItemCommand command) {
        logger.info("Packing item in session: {}", command.sessionId());

        PackingSession session = findSessionById(command.sessionId());

        // Find or create container
        Container container = findOrCreateContainer(session, command.containerId(), command.instructionId());

        // Pack the item
        session.packItem(command.instructionId(), container.getContainerId(), command.quantity());

        logger.info("Successfully packed item {} into container {}",
                command.instructionId(), command.containerId());

        return sessionRepository.save(session);
    }

    /**
     * Seal a container
     */
    @Transactional
    public PackingSession sealContainer(SealContainerCommand command) {
        logger.info("Sealing container {} in session {}",
                command.containerId(), command.sessionId());

        PackingSession session = findSessionById(command.sessionId());
        session.sealContainer(command.containerId(), command.actualWeight());

        logger.info("Container {} sealed successfully", command.containerId());

        return sessionRepository.save(session);
    }

    /**
     * Complete packing session
     */
    @Transactional
    public PackingSession completeSession(String sessionId) {
        logger.info("Completing packing session: {}", sessionId);

        PackingSession session = findSessionById(sessionId);
        session.complete();

        logger.info("Packing session {} completed successfully", sessionId);

        return sessionRepository.save(session);
    }

    /**
     * Cancel packing session
     */
    @Transactional
    public PackingSession cancelSession(String sessionId, String reason) {
        logger.info("Cancelling packing session: {} for reason: {}", sessionId, reason);

        PackingSession session = findSessionById(sessionId);
        session.cancel(reason);

        return sessionRepository.save(session);
    }

    /**
     * Mark item as missing
     */
    @Transactional
    public PackingSession markItemMissing(String sessionId, String instructionId, String reason) {
        logger.info("Marking item {} as missing in session {}", instructionId, sessionId);

        PackingSession session = findSessionById(sessionId);
        session.markItemMissing(instructionId, reason);

        return sessionRepository.save(session);
    }

    /**
     * Mark item as damaged
     */
    @Transactional
    public PackingSession markItemDamaged(String sessionId, String instructionId, String reason) {
        logger.info("Marking item {} as damaged in session {}", instructionId, sessionId);

        PackingSession session = findSessionById(sessionId);
        session.markItemDamaged(instructionId, reason);

        return sessionRepository.save(session);
    }

    /**
     * Get current instruction to pack
     */
    public PackingInstruction getCurrentInstruction(String sessionId) {
        PackingSession session = findSessionById(sessionId);
        return session.getCurrentInstruction();
    }

    /**
     * Get active session for worker
     */
    public Optional<PackingSession> getActiveSessionForWorker(String workerId) {
        return sessionRepository.findActiveSessionByWorkerId(workerId);
    }

    /**
     * Get session progress
     */
    public double getSessionProgress(String sessionId) {
        PackingSession session = findSessionById(sessionId);
        return session.getProgress();
    }

    /**
     * Get all active sessions in warehouse
     */
    public List<PackingSession> getActiveSessionsByWarehouse(String warehouseId) {
        return sessionRepository.findActiveSessionsByWarehouse(warehouseId);
    }

    /**
     * Get session by ID
     */
    public PackingSession getSession(String sessionId) {
        return findSessionById(sessionId);
    }

    // ========== Sprint 1: Scanning Workflow ==========

    /**
     * Initialize scanning workflow
     */
    @Transactional
    public PackingSession initializeScanning(String sessionId) {
        logger.info("Initializing scanning for session: {}", sessionId);

        PackingSession session = findSessionById(sessionId);
        session.initializeItemsToScan();

        logger.info("Scanning initialized for session: {}", sessionId);

        return sessionRepository.save(session);
    }

    /**
     * Scan an item
     */
    @Transactional
    public PackingSession scanItem(ScanItemCommand command) {
        logger.info("Scanning item {} in session {}", command.barcode(), command.sessionId());

        PackingSession session = findSessionById(command.sessionId());
        session.scanItem(command.barcode());

        logger.info("Item scanned successfully: {}", command.barcode());

        return sessionRepository.save(session);
    }

    // ========== Sprint 1: Carton Selection ==========

    /**
     * Recommend carton for session
     */
    public String recommendCarton(String sessionId) {
        logger.debug("Recommending carton for session: {}", sessionId);

        PackingSession session = findSessionById(sessionId);
        return session.recommendCarton();
    }

    /**
     * Select carton for packing
     */
    @Transactional
    public PackingSession selectCarton(SelectCartonCommand command) {
        logger.info("Selecting carton {} for session {}", command.cartonType(), command.sessionId());

        PackingSession session = findSessionById(command.sessionId());
        session.selectCarton(command.cartonType());

        logger.info("Carton selected successfully: {}", command.cartonType());

        return sessionRepository.save(session);
    }

    // ========== Sprint 1: Weight Verification ==========

    /**
     * Weigh and close package
     */
    @Transactional
    public PackingSession weighPackage(WeighPackageCommand command) {
        logger.info("Weighing package in session {}", command.sessionId());

        PackingSession session = findSessionById(command.sessionId());
        session.weighAndClose(command.actualWeight());

        logger.info("Package weighed successfully: {} lb", command.actualWeight().toPounds());

        return sessionRepository.save(session);
    }

    // ========== Sprint 1: Quality Control ==========

    /**
     * Perform quality check
     */
    @Transactional
    public PackingSession performQualityCheck(PerformQualityCheckCommand command) {
        logger.info("Performing quality check for session {}", command.sessionId());

        PackingSession session = findSessionById(command.sessionId());

        if (command.photoUrls() != null && !command.photoUrls().isEmpty()) {
            session.performQualityCheck(command.checkerId(), command.checkpoints(), command.photoUrls());
        } else {
            session.performQualityCheck(command.checkerId(), command.checkpoints());
        }

        logger.info("Quality check completed: {}", session.getQualityCheck().isPassed() ? "PASSED" : "FAILED");

        return sessionRepository.save(session);
    }

    // Private helper methods

    private PackingSession findSessionById(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    private Container findOrCreateContainer(PackingSession session, String containerId, String instructionId) {
        // If specific container ID provided, use it
        if (containerId != null) {
            return session.getContainers().stream()
                    .filter(c -> c.getContainerId().equals(containerId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Container not found: " + containerId));
        }

        // Auto-select container for instruction
        PackingInstruction instruction = session.getPackingInstructions().stream()
                .filter(i -> i.getInstructionId().equals(instructionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Instruction not found: " + instructionId));

        // Try to find existing suitable container
        Container suitable = containerOptimizationService.selectBestContainer(
                instruction,
                session.getContainers()
        );

        if (suitable != null) {
            return suitable;
        }

        // Create new container
        ContainerType recommendedType = containerOptimizationService.recommendContainer(
                List.of(instruction)
        );
        Container newContainer = Container.create(recommendedType);
        session.addContainer(newContainer);

        logger.info("Created new container {} for item {}", newContainer.getContainerId(), instructionId);

        return newContainer;
    }
}
