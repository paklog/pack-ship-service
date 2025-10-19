package com.paklog.wes.pack.domain.aggregate;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.pack.domain.entity.Container;
import com.paklog.wes.pack.domain.entity.PackingInstruction;
import com.paklog.wes.pack.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PackingSession aggregate
 */
@DisplayName("PackingSession Tests")
class PackingSessionTest {

    @Test
    @DisplayName("Should create packing session successfully")
    void shouldCreatePackingSession() {
        // Given
        List<PackingInstruction> instructions = createTestInstructions(3);

        // When
        PackingSession session = PackingSession.create(
                "SESSION-001",
                "ORDER-001",
                "WORKER-001",
                "WH-001",
                instructions
        );

        // Then
        assertThat(session.getSessionId()).isNotNull();
        assertThat(session.getOrderId()).isEqualTo("ORDER-001");
        assertThat(session.getWorkerId()).isEqualTo("WORKER-001");
        assertThat(session.getStatus()).isEqualTo(PackingStatus.CREATED);
        assertThat(session.getPackingInstructions()).hasSize(3);
    }

    @Test
    @DisplayName("Should reject session creation without instructions")
    void shouldRejectCreationWithoutInstructions() {
        // When/Then
        assertThatThrownBy(() -> PackingSession.create(
                "SESSION-001",
                "ORDER-001",
                "WORKER-001",
                "WH-001",
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("at least one");
    }

    @Test
    @DisplayName("Should start packing session")
    void shouldStartSession() {
        // Given
        List<PackingInstruction> instructions = createTestInstructions(3);
        PackingSession session = PackingSession.create(
                "SESSION-001", "ORDER-001", "WORKER-001", "WH-001", instructions
        );

        // When
        session.start();

        // Then
        assertThat(session.getStatus()).isEqualTo(PackingStatus.IN_PROGRESS);
        assertThat(session.getStartedAt()).isNotNull();
        assertThat(session.getDomainEvents()).isNotEmpty();
    }

    @Test
    @DisplayName("Should pack item into container")
    void shouldPackItem() {
        // Given
        PackingSession session = createStartedSession(2);
        Container container = Container.create(ContainerType.MEDIUM_BOX);
        session.addContainer(container);
        PackingInstruction instruction = session.getCurrentInstruction();

        // When
        session.packItem(instruction.getInstructionId(), container.getContainerId(), instruction.getExpectedQuantity());

        // Then
        assertThat(instruction.getStatus()).isEqualTo(PackingInstruction.InstructionStatus.PACKED);
        assertThat(instruction.getPackedQuantity()).isEqualTo(instruction.getExpectedQuantity());
        assertThat(container.getItemCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should seal container")
    void shouldSealContainer() {
        // Given
        PackingSession session = createStartedSession(2); // Create with 2 items to prevent auto-complete
        Container container = Container.create(ContainerType.MEDIUM_BOX);
        session.addContainer(container);
        PackingInstruction instruction = session.getCurrentInstruction();
        session.packItem(instruction.getInstructionId(), container.getContainerId(), instruction.getExpectedQuantity());

        Weight actualWeight = new Weight(5.0, Weight.WeightUnit.LB);

        // When
        session.sealContainer(container.getContainerId(), actualWeight);

        // Then
        assertThat(container.getStatus()).isEqualTo(Container.ContainerStatus.SEALED);
        assertThat(container.getSealedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should auto-complete when all items packed")
    void shouldAutoCompleteSession() {
        // Given
        PackingSession session = createStartedSession(2);
        Container container = Container.create(ContainerType.MEDIUM_BOX);
        session.addContainer(container);

        // When - Pack all items
        for (PackingInstruction instruction : session.getPackingInstructions()) {
            session.packItem(instruction.getInstructionId(), container.getContainerId(), instruction.getExpectedQuantity());
        }

        // Then - Should auto-complete
        assertThat(session.getStatus()).isEqualTo(PackingStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should cancel packing session")
    void shouldCancelSession() {
        // Given
        PackingSession session = createStartedSession(2);

        // When
        session.cancel("Worker unavailable");

        // Then
        assertThat(session.getStatus()).isEqualTo(PackingStatus.CANCELLED);
        assertThat(session.getCancellationReason()).isEqualTo("Worker unavailable");
        assertThat(session.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should mark item as missing")
    void shouldMarkItemMissing() {
        // Given
        PackingSession session = createStartedSession(2);
        PackingInstruction instruction = session.getCurrentInstruction();

        // When
        session.markItemMissing(instruction.getInstructionId(), "Item not found");

        // Then
        assertThat(instruction.getStatus()).isEqualTo(PackingInstruction.InstructionStatus.MISSING);
        assertThat(instruction.getNotes()).isEqualTo("Item not found");
    }

    @Test
    @DisplayName("Should mark item as damaged")
    void shouldMarkItemDamaged() {
        // Given
        PackingSession session = createStartedSession(2);
        PackingInstruction instruction = session.getCurrentInstruction();

        // When
        session.markItemDamaged(instruction.getInstructionId(), "Item broken during pick");

        // Then
        assertThat(instruction.getStatus()).isEqualTo(PackingInstruction.InstructionStatus.DAMAGED);
        assertThat(instruction.getNotes()).isEqualTo("Item broken during pick");
    }

    @Test
    @DisplayName("Should calculate progress correctly")
    void shouldCalculateProgress() {
        // Given
        PackingSession session = createStartedSession(4);
        Container container = Container.create(ContainerType.LARGE_BOX);
        session.addContainer(container);

        // When - Pack 2 out of 4
        List<PackingInstruction> instructions = new ArrayList<>(session.getPackingInstructions());
        session.packItem(instructions.get(0).getInstructionId(), container.getContainerId(), instructions.get(0).getExpectedQuantity());
        session.packItem(instructions.get(1).getInstructionId(), container.getContainerId(), instructions.get(1).getExpectedQuantity());

        // Then
        assertThat(session.getProgress()).isEqualTo(50.0);
        assertThat(session.getPackedInstructionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should calculate accuracy correctly")
    void shouldCalculateAccuracy() {
        // Given
        PackingSession session = createStartedSession(3);
        Container container = Container.create(ContainerType.LARGE_BOX);
        session.addContainer(container);
        List<PackingInstruction> instructions = new ArrayList<>(session.getPackingInstructions());

        // When - Pack 2, mark 1 missing
        session.packItem(instructions.get(0).getInstructionId(), container.getContainerId(), instructions.get(0).getExpectedQuantity());
        session.packItem(instructions.get(1).getInstructionId(), container.getContainerId(), instructions.get(1).getExpectedQuantity());
        session.markItemMissing(instructions.get(2).getInstructionId(), "missing");

        // Then
        double accuracy = session.calculateAccuracy();
        assertThat(accuracy).isBetween(66.0, 67.0); // 2 packed out of 3 total (~66.67%)
    }

    @Test
    @DisplayName("Should get current instruction")
    void shouldGetCurrentInstruction() {
        // Given
        PackingSession session = createStartedSession(3);

        // When
        PackingInstruction current = session.getCurrentInstruction();

        // Then
        assertThat(current).isNotNull();
        assertThat(current.getStatus()).isEqualTo(PackingInstruction.InstructionStatus.PENDING);
    }

    @Test
    @DisplayName("Should validate state transitions")
    void shouldValidateStateTransitions() {
        // Given
        PackingSession session = createStartedSession(2);

        // When/Then - Cannot start already started session
        assertThatThrownBy(() -> session.start())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Should calculate session duration")
    void shouldCalculateDuration() throws InterruptedException {
        // Given
        PackingSession session = createStartedSession(1);

        // When
        Thread.sleep(100); // Wait a bit
        Duration duration = session.getDuration();

        // Then
        assertThat(duration).isNotNull();
        assertThat(duration.toMillis()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should track total weight")
    void shouldTrackTotalWeight() {
        // Given
        PackingSession session = createStartedSession(2);
        Container container = Container.create(ContainerType.MEDIUM_BOX);
        session.addContainer(container);

        // When - Pack items
        for (PackingInstruction instruction : session.getPackingInstructions()) {
            session.packItem(instruction.getInstructionId(), container.getContainerId(), instruction.getExpectedQuantity());
        }

        // Then
        double totalWeight = session.getTotalWeight();
        assertThat(totalWeight).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should get container count")
    void shouldGetContainerCount() {
        // Given
        PackingSession session = createStartedSession(2);

        // When - Add containers
        session.addContainer(Container.create(ContainerType.SMALL_BOX));
        session.addContainer(Container.create(ContainerType.MEDIUM_BOX));

        // Then
        assertThat(session.getContainers()).hasSize(2);
    }

    // Helper methods

    private List<PackingInstruction> createTestInstructions(int count) {
        List<PackingInstruction> instructions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Weight itemWeight = new Weight(0.5, Weight.WeightUnit.LB);
            Dimensions itemDims = new Dimensions(6, 4, 2, Dimensions.DimensionUnit.IN);

            PackingInstruction instruction = new PackingInstruction(
                    "INST-" + (i + 1),
                    "SKU-" + (i + 1),
                    "Item " + (i + 1),
                    10,
                    itemWeight,
                    itemDims,
                    "ORDER-001",
                    Priority.NORMAL
            );
            instructions.add(instruction);
        }
        return instructions;
    }

    private PackingSession createStartedSession(int instructionCount) {
        List<PackingInstruction> instructions = createTestInstructions(instructionCount);
        PackingSession session = PackingSession.create(
                "SESSION-001", "ORDER-001", "WORKER-001", "WH-001", instructions
        );
        session.start();
        return session;
    }

    // ========== Sprint 1 Tests ==========

    @Test
    @DisplayName("Should initialize scanning workflow")
    void shouldInitializeScanning() {
        // Given
        List<PackingInstruction> instructions = createTestInstructionsWithBarcodes(3);
        PackingSession session = PackingSession.create(
                "SESSION-001", "ORDER-001", "WORKER-001", "WH-001", instructions
        );

        // When
        session.initializeItemsToScan();

        // Then
        assertThat(session.getStatus()).isEqualTo(PackingStatus.SCANNING);
        assertThat(session.getItemsToScan()).hasSize(3);
        assertThat(session.getScanningProgress()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should scan items successfully")
    void shouldScanItems() {
        // Given
        List<PackingInstruction> instructions = createTestInstructionsWithBarcodes(2);
        PackingSession session = PackingSession.create(
                "SESSION-001", "ORDER-001", "WORKER-001", "WH-001", instructions
        );
        session.initializeItemsToScan();

        // When
        for (int i = 0; i < 10; i++) { // Scan first item's expected quantity
            session.scanItem("BARCODE-1");
        }

        // Then
        assertThat(session.getScannedItems()).hasSize(10);
        assertThat(session.getScanningProgress()).isEqualTo(50.0); // 1 of 2 items
    }

    @Test
    @DisplayName("Should transition to READY_FOR_CARTON after all items scanned")
    void shouldTransitionAfterScanningComplete() {
        // Given
        List<PackingInstruction> instructions = createTestInstructionsWithBarcodes(2);
        PackingSession session = PackingSession.create(
                "SESSION-001", "ORDER-001", "WORKER-001", "WH-001", instructions
        );
        session.initializeItemsToScan();

        // When - Scan all items
        for (int i = 0; i < 10; i++) {
            session.scanItem("BARCODE-1");
        }
        for (int i = 0; i < 10; i++) {
            session.scanItem("BARCODE-2");
        }

        // Then
        assertThat(session.getStatus()).isEqualTo(PackingStatus.READY_FOR_CARTON);
        assertThat(session.allItemsScanned()).isTrue();
    }

    @Test
    @DisplayName("Should recommend carton based on item count")
    void shouldRecommendCarton() {
        // Given
        List<PackingInstruction> instructions = createTestInstructionsWithBarcodes(3);
        PackingSession session = PackingSession.create(
                "SESSION-001", "ORDER-001", "WORKER-001", "WH-001", instructions
        );
        session.initializeItemsToScan();
        scanAllItems(session);

        // When
        String recommendation = session.recommendCarton();

        // Then
        assertThat(recommendation).isEqualTo("LARGE_BOX"); // 30 items total (3 * 10)
    }

    @Test
    @DisplayName("Should select suitable carton")
    void shouldSelectCarton() {
        // Given
        List<PackingInstruction> instructions = createTestInstructionsWithBarcodes(1);
        PackingSession session = PackingSession.create(
                "SESSION-001", "ORDER-001", "WORKER-001", "WH-001", instructions
        );
        session.initializeItemsToScan();
        scanAllItems(session);

        // When
        session.selectCarton("MEDIUM_BOX");

        // Then
        assertThat(session.getSelectedCarton()).isEqualTo("MEDIUM_BOX");
        assertThat(session.getStatus()).isEqualTo(PackingStatus.READY_TO_PACK);
        assertThat(session.getPackingMaterials()).isNotEmpty();
    }

    @Test
    @DisplayName("Should weigh package within tolerance")
    void shouldWeighPackageWithinTolerance() {
        // Given
        PackingSession session = createStartedSession(3);
        Container container = Container.create(ContainerType.MEDIUM_BOX);
        session.addContainer(container);

        // Pack only 2 items, leave 1 unpacked to prevent auto-complete
        List<PackingInstruction> instructions = new ArrayList<>(session.getPackingInstructions());
        session.packItem(instructions.get(0).getInstructionId(), container.getContainerId(), instructions.get(0).getExpectedQuantity());
        session.packItem(instructions.get(1).getInstructionId(), container.getContainerId(), instructions.get(1).getExpectedQuantity());

        // When - Weigh with 3% discrepancy (within 5% tolerance)
        double estimatedWeight = instructions.get(0).getTotalWeight().toPounds() +
                                 instructions.get(1).getTotalWeight().toPounds();
        Weight actualWeight = new Weight(estimatedWeight * 1.03); // 3% heavier

        session.weighAndClose(actualWeight);

        // Then
        assertThat(session.getStatus()).isEqualTo(PackingStatus.READY_TO_SHIP);
        assertThat(session.getActualWeight()).isNotNull();
    }

    @Test
    @DisplayName("Should perform quality check")
    void shouldPerformQualityCheck() {
        // Given
        PackingSession session = createStartedSession(2);
        Container container = Container.create(ContainerType.MEDIUM_BOX);
        session.addContainer(container);

        // Pack only first item to prevent auto-complete
        PackingInstruction instruction = session.getCurrentInstruction();
        session.packItem(instruction.getInstructionId(), container.getContainerId(), instruction.getExpectedQuantity());

        // When
        List<com.paklog.wes.pack.domain.entity.QualityCheck.Checkpoint> checkpoints = List.of(
                new com.paklog.wes.pack.domain.entity.QualityCheck.Checkpoint("Item Condition", true, "Good"),
                new com.paklog.wes.pack.domain.entity.QualityCheck.Checkpoint("Packaging Quality", true, "Excellent")
        );
        session.performQualityCheck("QC-001", checkpoints);

        // Then
        assertThat(session.getQualityCheck()).isNotNull();
        assertThat(session.getQualityCheck().isPassed()).isTrue();
        assertThat(session.getStatus()).isEqualTo(PackingStatus.QC_PASSED);
    }

    @Test
    @DisplayName("Should handle failed quality check")
    void shouldHandleFailedQualityCheck() {
        // Given
        PackingSession session = createStartedSession(2);
        Container container = Container.create(ContainerType.MEDIUM_BOX);
        session.addContainer(container);

        // Pack only first item to prevent auto-complete
        PackingInstruction instruction = session.getCurrentInstruction();
        session.packItem(instruction.getInstructionId(), container.getContainerId(), instruction.getExpectedQuantity());

        // When - Fail one checkpoint
        List<com.paklog.wes.pack.domain.entity.QualityCheck.Checkpoint> checkpoints = List.of(
                new com.paklog.wes.pack.domain.entity.QualityCheck.Checkpoint("Item Condition", true, "Good"),
                new com.paklog.wes.pack.domain.entity.QualityCheck.Checkpoint("Packaging Quality", false, "Damaged corner")
        );
        session.performQualityCheck("QC-001", checkpoints);

        // Then
        assertThat(session.getQualityCheck().isPassed()).isFalse();
        assertThat(session.getStatus()).isEqualTo(PackingStatus.PACKING); // Reset for rework
    }

    // Sprint 1 Helper Methods

    private List<PackingInstruction> createTestInstructionsWithBarcodes(int count) {
        List<PackingInstruction> instructions = createTestInstructions(count);
        for (int i = 0; i < instructions.size(); i++) {
            instructions.get(i).setBarcode("BARCODE-" + (i + 1));
        }
        return instructions;
    }

    private void scanAllItems(PackingSession session) {
        for (int i = 1; i <= session.getItemsToScan().size(); i++) {
            for (int j = 0; j < 10; j++) { // Expected quantity is 10
                session.scanItem("BARCODE-" + i);
            }
        }
    }
}
