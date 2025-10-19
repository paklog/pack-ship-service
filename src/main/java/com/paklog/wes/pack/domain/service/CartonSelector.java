package com.paklog.wes.pack.domain.service;

import com.paklog.wes.pack.domain.entity.Carton;
import com.paklog.wes.pack.domain.entity.PackItem;
import com.paklog.wes.pack.domain.valueobject.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service for optimal carton selection using 3D bin packing algorithm
 * Minimizes shipping costs while ensuring items fit properly
 */
@Service
public class CartonSelector {

    private static final Logger logger = LoggerFactory.getLogger(CartonSelector.class);

    private static final BigDecimal DIM_WEIGHT_DIVISOR = new BigDecimal("166"); // Standard for domestic shipping
    private static final BigDecimal MIN_FILL_RATE = new BigDecimal("0.40"); // 40% minimum utilization
    private static final BigDecimal OPTIMAL_FILL_RATE = new BigDecimal("0.75"); // 75% target utilization

    private final List<CartonType> availableCartons;

    public CartonSelector(List<CartonType> availableCartons) {
        this.availableCartons = availableCartons != null ? availableCartons : getStandardCartons();
    }

    /**
     * Select optimal carton for given items
     * Considers: item dimensions, weight, fragility, cost optimization
     */
    public Carton selectOptimalCarton(List<PackItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cannot select carton for empty items");
        }

        logger.info("Selecting optimal carton for {} items", items.size());

        // Calculate total metrics
        ItemMetrics metrics = calculateItemMetrics(items);

        // Find cartons that can physically fit items
        List<CartonType> candidateCartons = findCandidateCartons(items, metrics);

        if (candidateCartons.isEmpty()) {
            logger.warn("No single carton can fit all items, need multi-carton solution");
            return null; // Caller should split items
        }

        // Score each candidate carton
        Map<CartonType, CartonScore> scores = scoreCandidates(candidateCartons, metrics, items);

        // Select carton with best score
        CartonType bestCarton = scores.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(candidateCartons.get(0));

        CartonScore bestScore = scores.get(bestCarton);
        logger.info("Selected {} with score {}", bestCarton.getName(), bestScore.getTotalScore());

        return createCarton(bestCarton, items, metrics);
    }

    /**
     * Try 3D bin packing to verify items can fit
     */
    public PackingResult tryPack3D(List<PackItem> items, CartonType cartonType) {
        logger.debug("Attempting 3D bin packing for carton: {}", cartonType.getName());

        // Sort items by volume (largest first)
        List<PackItem> sortedItems = items.stream()
                .sorted(Comparator.comparing(PackItem::getVolume).reversed())
                .collect(Collectors.toList());

        List<PlacedItem> placements = new ArrayList<>();
        List<Space> availableSpaces = new ArrayList<>();

        // Initialize with full carton space
        availableSpaces.add(new Space(
                0, 0, 0,
                cartonType.getLength().doubleValue(),
                cartonType.getWidth().doubleValue(),
                cartonType.getHeight().doubleValue()
        ));

        // Try to place each item
        for (PackItem item : sortedItems) {
            boolean placed = false;

            // Try all available spaces
            for (int i = 0; i < availableSpaces.size(); i++) {
                Space space = availableSpaces.get(i);

                // Try all orientations
                List<Orientation> orientations = generateOrientations(item);

                for (Orientation orientation : orientations) {
                    if (canFitInSpace(orientation, space)) {
                        // Place item in this space
                        PlacedItem placement = new PlacedItem(
                                item,
                                space.x,
                                space.y,
                                space.z,
                                orientation
                        );
                        placements.add(placement);

                        // Remove used space and add remaining spaces
                        availableSpaces.remove(i);
                        availableSpaces.addAll(splitSpace(space, orientation));

                        placed = true;
                        break;
                    }
                }

                if (placed) break;
            }

            if (!placed) {
                logger.debug("Could not pack item: {}", item.getProductId());
                return PackingResult.failure("Item does not fit: " + item.getProductId());
            }
        }

        logger.debug("Successfully packed all {} items", items.size());
        return PackingResult.success(placements);
    }

    /**
     * Split orders into multiple cartons if needed
     */
    public List<Carton> splitIntoMultipleCartons(List<PackItem> items) {
        logger.info("Splitting {} items into multiple cartons", items.size());

        List<Carton> cartons = new ArrayList<>();
        List<PackItem> remainingItems = new ArrayList<>(items);

        int attempt = 0;
        final int MAX_ATTEMPTS = 10;

        while (!remainingItems.isEmpty() && attempt < MAX_ATTEMPTS) {
            attempt++;

            // Start with all remaining items
            List<PackItem> currentBatch = new ArrayList<>(remainingItems);

            // Try to find a carton that fits
            Carton carton = null;
            while (carton == null && !currentBatch.isEmpty()) {
                carton = selectOptimalCarton(currentBatch);

                if (carton == null) {
                    // Remove largest item and try again
                    PackItem largest = currentBatch.stream()
                            .max(Comparator.comparing(PackItem::getVolume))
                            .orElse(currentBatch.get(currentBatch.size() - 1));
                    currentBatch.remove(largest);
                }
            }

            if (carton != null) {
                cartons.add(carton);
                remainingItems.removeAll(currentBatch);
                logger.debug("Packed {} items into carton {}", currentBatch.size(), carton.getCartonType());
            } else {
                logger.error("Cannot pack items even after splitting");
                break;
            }
        }

        if (!remainingItems.isEmpty()) {
            logger.warn("{} items could not be packed", remainingItems.size());
        }

        logger.info("Split into {} cartons", cartons.size());
        return cartons;
    }

    /**
     * Suggest alternative cartons with pros/cons
     */
    public List<CartonSuggestion> suggestAlternatives(List<PackItem> items) {
        ItemMetrics metrics = calculateItemMetrics(items);
        List<CartonType> candidates = findCandidateCartons(items, metrics);
        Map<CartonType, CartonScore> scores = scoreCandidates(candidates, metrics, items);

        return scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> new CartonSuggestion(
                        entry.getKey(),
                        entry.getValue(),
                        generateRecommendation(entry.getKey(), entry.getValue())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Calculate metrics for all items
     */
    private ItemMetrics calculateItemMetrics(List<PackItem> items) {
        BigDecimal totalVolume = items.stream()
                .map(PackItem::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWeight = items.stream()
                .map(PackItem::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasFragile = items.stream()
                .anyMatch(PackItem::isFragile);

        boolean requiresPadding = items.stream()
                .anyMatch(PackItem::isRequiresPadding);

        // Calculate bounding box (minimum dimensions needed)
        BigDecimal maxLength = items.stream()
                .map(item -> item.getDimensions().getLength())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxWidth = items.stream()
                .map(item -> item.getDimensions().getWidth())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalHeight = items.stream()
                .map(item -> item.getDimensions().getHeight())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ItemMetrics(
                totalVolume,
                totalWeight,
                maxLength,
                maxWidth,
                totalHeight,
                hasFragile,
                requiresPadding,
                items.size()
        );
    }

    /**
     * Find cartons that can potentially fit items
     */
    private List<CartonType> findCandidateCartons(List<PackItem> items, ItemMetrics metrics) {
        return availableCartons.stream()
                .filter(carton -> {
                    // Weight check
                    if (metrics.totalWeight.compareTo(carton.getMaxWeight()) > 0) {
                        return false;
                    }

                    // Volume check (with 80% utilization assumption)
                    BigDecimal cartonVolume = carton.getLength()
                            .multiply(carton.getWidth())
                            .multiply(carton.getHeight());

                    BigDecimal requiredVolume = metrics.totalVolume.divide(
                            new BigDecimal("0.80"),
                            2,
                            RoundingMode.HALF_UP
                    );

                    if (cartonVolume.compareTo(requiredVolume) < 0) {
                        return false;
                    }

                    // Try 3D packing
                    PackingResult result = tryPack3D(items, carton);
                    return result.isSuccess();
                })
                .collect(Collectors.toList());
    }

    /**
     * Score each candidate carton
     */
    private Map<CartonType, CartonScore> scoreCandidates(
            List<CartonType> candidates,
            ItemMetrics metrics,
            List<PackItem> items) {

        Map<CartonType, CartonScore> scores = new HashMap<>();

        for (CartonType carton : candidates) {
            CartonScore score = calculateScore(carton, metrics, items);
            scores.put(carton, score);
        }

        return scores;
    }

    /**
     * Calculate comprehensive score for carton selection
     */
    private CartonScore calculateScore(CartonType carton, ItemMetrics metrics, List<PackItem> items) {
        // 1. Volume utilization (40% weight)
        BigDecimal cartonVolume = carton.getLength()
                .multiply(carton.getWidth())
                .multiply(carton.getHeight());
        BigDecimal fillRate = metrics.totalVolume.divide(cartonVolume, 4, RoundingMode.HALF_UP);
        double utilizationScore = Math.min(100, fillRate.doubleValue() / OPTIMAL_FILL_RATE.doubleValue() * 100);

        // 2. Dimensional weight efficiency (30% weight)
        BigDecimal dimWeight = calculateDimensionalWeight(carton);
        BigDecimal actualWeight = metrics.totalWeight.add(carton.getTareWeight());
        BigDecimal chargeableWeight = dimWeight.max(actualWeight);

        double weightScore = 100 * (1 - (chargeableWeight.subtract(actualWeight)
                .divide(actualWeight.add(BigDecimal.ONE), 4, RoundingMode.HALF_UP)
                .doubleValue()));

        // 3. Material cost efficiency (20% weight)
        double costScore = 100 * (1 - (carton.getCost().doubleValue() / 10.0)); // Normalize to 0-100

        // 4. Protection adequacy (10% weight)
        double protectionScore = 100.0;
        if (metrics.hasFragile && carton.getName().contains("ENVELOPE")) {
            protectionScore = 0.0; // Envelopes bad for fragile items
        }

        // Combine scores
        double totalScore = (utilizationScore * 0.40)
                + (weightScore * 0.30)
                + (costScore * 0.20)
                + (protectionScore * 0.10);

        return new CartonScore(
                totalScore,
                utilizationScore,
                weightScore,
                costScore,
                protectionScore,
                fillRate,
                chargeableWeight
        );
    }

    /**
     * Calculate dimensional weight for shipping
     */
    private BigDecimal calculateDimensionalWeight(CartonType carton) {
        return carton.getLength()
                .multiply(carton.getWidth())
                .multiply(carton.getHeight())
                .divide(DIM_WEIGHT_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    /**
     * Create carton instance
     */
    private Carton createCarton(CartonType type, List<PackItem> items, ItemMetrics metrics) {
        Carton carton = new Carton();
        carton.setCartonType(type);
        carton.setDimensions(new Dimensions(type.getLength(), type.getWidth(), type.getHeight()));
        carton.setMaxWeight(type.getMaxWeight());
        carton.setTareWeight(type.getTareWeight());

        // Add all items
        for (PackItem item : items) {
            carton.addItem(item);
        }

        return carton;
    }

    /**
     * Generate all possible orientations for an item
     */
    private List<Orientation> generateOrientations(PackItem item) {
        Dimensions dim = item.getDimensions();
        List<Orientation> orientations = new ArrayList<>();

        // All 6 possible orientations
        orientations.add(new Orientation(dim.getLength(), dim.getWidth(), dim.getHeight()));
        orientations.add(new Orientation(dim.getLength(), dim.getHeight(), dim.getWidth()));
        orientations.add(new Orientation(dim.getWidth(), dim.getLength(), dim.getHeight()));
        orientations.add(new Orientation(dim.getWidth(), dim.getHeight(), dim.getLength()));
        orientations.add(new Orientation(dim.getHeight(), dim.getLength(), dim.getWidth()));
        orientations.add(new Orientation(dim.getHeight(), dim.getWidth(), dim.getLength()));

        return orientations;
    }

    /**
     * Check if orientation fits in space
     */
    private boolean canFitInSpace(Orientation orientation, Space space) {
        return orientation.length.doubleValue() <= space.length
                && orientation.width.doubleValue() <= space.width
                && orientation.height.doubleValue() <= space.height;
    }

    /**
     * Split space after placing item
     */
    private List<Space> splitSpace(Space originalSpace, Orientation placedItem) {
        List<Space> newSpaces = new ArrayList<>();

        double itemLength = placedItem.length.doubleValue();
        double itemWidth = placedItem.width.doubleValue();
        double itemHeight = placedItem.height.doubleValue();

        // Create remaining spaces around placed item
        // Space to the right
        if (originalSpace.length > itemLength) {
            newSpaces.add(new Space(
                    originalSpace.x + itemLength,
                    originalSpace.y,
                    originalSpace.z,
                    originalSpace.length - itemLength,
                    originalSpace.width,
                    originalSpace.height
            ));
        }

        // Space above
        if (originalSpace.width > itemWidth) {
            newSpaces.add(new Space(
                    originalSpace.x,
                    originalSpace.y + itemWidth,
                    originalSpace.z,
                    itemLength,
                    originalSpace.width - itemWidth,
                    originalSpace.height
            ));
        }

        // Space in front
        if (originalSpace.height > itemHeight) {
            newSpaces.add(new Space(
                    originalSpace.x,
                    originalSpace.y,
                    originalSpace.z + itemHeight,
                    itemLength,
                    itemWidth,
                    originalSpace.height - itemHeight
            ));
        }

        return newSpaces;
    }

    /**
     * Generate recommendation text
     */
    private String generateRecommendation(CartonType carton, CartonScore score) {
        if (score.getTotalScore() > 85) {
            return "Excellent choice - optimal fit and cost efficiency";
        } else if (score.getTotalScore() > 70) {
            return "Good option - acceptable utilization";
        } else if (score.getFillRate().doubleValue() < 0.40) {
            return "Warning - low utilization, consider smaller carton";
        } else {
            return "Marginal fit - consider alternatives";
        }
    }

    /**
     * Standard carton types
     */
    private List<CartonType> getStandardCartons() {
        List<CartonType> cartons = new ArrayList<>();

        cartons.add(new CartonType("SMALL_BOX", new BigDecimal("8"), new BigDecimal("6"), new BigDecimal("4"),
                new BigDecimal("20"), new BigDecimal("0.5"), new BigDecimal("0.50")));

        cartons.add(new CartonType("MEDIUM_BOX", new BigDecimal("12"), new BigDecimal("10"), new BigDecimal("8"),
                new BigDecimal("50"), new BigDecimal("1.0"), new BigDecimal("1.00")));

        cartons.add(new CartonType("LARGE_BOX", new BigDecimal("18"), new BigDecimal("14"), new BigDecimal("12"),
                new BigDecimal("100"), new BigDecimal("1.5"), new BigDecimal("1.50")));

        cartons.add(new CartonType("EXTRA_LARGE_BOX", new BigDecimal("24"), new BigDecimal("18"), new BigDecimal("18"),
                new BigDecimal("150"), new BigDecimal("2.0"), new BigDecimal("2.00")));

        return cartons;
    }

    // Supporting classes

    private static class ItemMetrics {
        final BigDecimal totalVolume;
        final BigDecimal totalWeight;
        final BigDecimal maxLength;
        final BigDecimal maxWidth;
        final BigDecimal totalHeight;
        final boolean hasFragile;
        final boolean requiresPadding;
        final int itemCount;

        ItemMetrics(BigDecimal totalVolume, BigDecimal totalWeight,
                    BigDecimal maxLength, BigDecimal maxWidth, BigDecimal totalHeight,
                    boolean hasFragile, boolean requiresPadding, int itemCount) {
            this.totalVolume = totalVolume;
            this.totalWeight = totalWeight;
            this.maxLength = maxLength;
            this.maxWidth = maxWidth;
            this.totalHeight = totalHeight;
            this.hasFragile = hasFragile;
            this.requiresPadding = requiresPadding;
            this.itemCount = itemCount;
        }
    }

    private static class Space {
        final double x, y, z;
        final double length, width, height;

        Space(double x, double y, double z, double length, double width, double height) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.length = length;
            this.width = width;
            this.height = height;
        }
    }

    private static class Orientation {
        final BigDecimal length, width, height;

        Orientation(BigDecimal length, BigDecimal width, BigDecimal height) {
            this.length = length;
            this.width = width;
            this.height = height;
        }
    }

    private static class PlacedItem {
        final PackItem item;
        final double x, y, z;
        final Orientation orientation;

        PlacedItem(PackItem item, double x, double y, double z, Orientation orientation) {
            this.item = item;
            this.x = x;
            this.y = y;
            this.z = z;
            this.orientation = orientation;
        }
    }

    public static class PackingResult {
        private final boolean success;
        private final String message;
        private final List<PlacedItem> placements;

        private PackingResult(boolean success, String message, List<PlacedItem> placements) {
            this.success = success;
            this.message = message;
            this.placements = placements;
        }

        public static PackingResult success(List<PlacedItem> placements) {
            return new PackingResult(true, "Packing successful", placements);
        }

        public static PackingResult failure(String message) {
            return new PackingResult(false, message, Collections.emptyList());
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public List<PlacedItem> getPlacements() {
            return placements;
        }
    }

    public static class CartonScore implements Comparable<CartonScore> {
        private final double totalScore;
        private final double utilizationScore;
        private final double weightScore;
        private final double costScore;
        private final double protectionScore;
        private final BigDecimal fillRate;
        private final BigDecimal chargeableWeight;

        public CartonScore(double totalScore, double utilizationScore, double weightScore,
                           double costScore, double protectionScore,
                           BigDecimal fillRate, BigDecimal chargeableWeight) {
            this.totalScore = totalScore;
            this.utilizationScore = utilizationScore;
            this.weightScore = weightScore;
            this.costScore = costScore;
            this.protectionScore = protectionScore;
            this.fillRate = fillRate;
            this.chargeableWeight = chargeableWeight;
        }

        public double getTotalScore() {
            return totalScore;
        }

        public BigDecimal getFillRate() {
            return fillRate;
        }

        @Override
        public int compareTo(CartonScore other) {
            return Double.compare(this.totalScore, other.totalScore);
        }
    }

    public static class CartonSuggestion {
        private final CartonType cartonType;
        private final CartonScore score;
        private final String recommendation;

        public CartonSuggestion(CartonType cartonType, CartonScore score, String recommendation) {
            this.cartonType = cartonType;
            this.score = score;
            this.recommendation = recommendation;
        }

        public CartonType getCartonType() {
            return cartonType;
        }

        public CartonScore getScore() {
            return score;
        }

        public String getRecommendation() {
            return recommendation;
        }
    }
}
