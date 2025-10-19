package com.paklog.wes.pack.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Quality check performed on packing session
 */
public class QualityCheck {

    public static class Checkpoint {
        private String name;
        private boolean passed;
        private String notes;

        public Checkpoint() {
            // For persistence
        }

        public Checkpoint(String name, boolean passed, String notes) {
            this.name = name;
            this.passed = passed;
            this.notes = notes;
        }

        public boolean isPassed() {
            return passed;
        }

        // Getters and setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean getPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        @Override
        public String toString() {
            return String.format("%s: %s%s",
                    name,
                    passed ? "PASS" : "FAIL",
                    notes != null && !notes.isBlank() ? " (" + notes + ")" : "");
        }
    }

    private String checkerId;
    private List<Checkpoint> checkpoints;
    private List<String> photoUrls;
    private LocalDateTime performedAt;
    private boolean passed;

    public QualityCheck() {
        // For persistence
        this.checkpoints = new ArrayList<>();
        this.photoUrls = new ArrayList<>();
    }

    public QualityCheck(String checkerId, List<Checkpoint> checkpoints, LocalDateTime performedAt) {
        this.checkerId = Objects.requireNonNull(checkerId, "Checker ID cannot be null");
        this.checkpoints = new ArrayList<>(Objects.requireNonNull(checkpoints, "Checkpoints cannot be null"));
        this.photoUrls = new ArrayList<>();
        this.performedAt = performedAt;
        this.passed = calculatePassed();
    }

    public QualityCheck(String checkerId, List<Checkpoint> checkpoints, List<String> photoUrls, LocalDateTime performedAt) {
        this.checkerId = Objects.requireNonNull(checkerId, "Checker ID cannot be null");
        this.checkpoints = new ArrayList<>(Objects.requireNonNull(checkpoints, "Checkpoints cannot be null"));
        this.photoUrls = photoUrls != null ? new ArrayList<>(photoUrls) : new ArrayList<>();
        this.performedAt = performedAt;
        this.passed = calculatePassed();
    }

    /**
     * Check if all checkpoints passed
     */
    private boolean calculatePassed() {
        return checkpoints.stream().allMatch(Checkpoint::isPassed);
    }

    /**
     * Check if QC failed
     */
    public boolean hasFailed() {
        return !passed;
    }

    /**
     * Get failed checkpoints
     */
    public List<Checkpoint> getFailedCheckpoints() {
        return checkpoints.stream()
                .filter(cp -> !cp.isPassed())
                .toList();
    }

    /**
     * Get pass rate
     */
    public double getPassRate() {
        if (checkpoints.isEmpty()) {
            return 100.0;
        }
        long passedCount = checkpoints.stream().filter(Checkpoint::isPassed).count();
        return (passedCount / (double) checkpoints.size()) * 100.0;
    }

    // Getters and setters

    public String getCheckerId() {
        return checkerId;
    }

    public void setCheckerId(String checkerId) {
        this.checkerId = checkerId;
    }

    public List<Checkpoint> getCheckpoints() {
        return checkpoints;
    }

    public void setCheckpoints(List<Checkpoint> checkpoints) {
        this.checkpoints = checkpoints;
        this.passed = calculatePassed();
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    public LocalDateTime getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(LocalDateTime performedAt) {
        this.performedAt = performedAt;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    @Override
    public String toString() {
        return "QualityCheck{" +
                "checkerId='" + checkerId + '\'' +
                ", passed=" + passed +
                ", checkpoints=" + checkpoints.size() +
                ", passRate=" + String.format("%.1f%%", getPassRate()) +
                '}';
    }
}
