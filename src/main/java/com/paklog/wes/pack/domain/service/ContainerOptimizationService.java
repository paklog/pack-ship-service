package com.paklog.wes.pack.domain.service;

import com.paklog.wes.pack.domain.entity.Container;
import com.paklog.wes.pack.domain.entity.PackingInstruction;
import com.paklog.wes.pack.domain.valueobject.ContainerType;
import com.paklog.wes.pack.domain.valueobject.Weight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Domain service for container optimization
 * Implements bin packing algorithm for container selection
 */
@Service
public class ContainerOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerOptimizationService.class);

    private static final double MAX_CONTAINER_WEIGHT_LB = 70.0; // Standard shipping limit

    /**
     * Recommend best container type for list of items
     */
    public ContainerType recommendContainer(List<PackingInstruction> items) {
        if (items == null || items.isEmpty()) {
            return ContainerType.SMALL_BOX;
        }

        logger.debug("Recommending container for {} items", items.size());

        // Calculate total weight
        double totalWeightLb = items.stream()
                .mapToDouble(i -> i.getTotalWeight().toPounds())
                .sum();

        // Calculate total volume (if dimensions available)
        double totalVolume = items.stream()
                .filter(i -> i.getItemDimensions() != null)
                .mapToDouble(i -> i.getItemDimensions().getVolumeInCubicInches())
                .sum();

        // Select container based on weight and volume
        ContainerType recommended = selectContainerByWeightAndVolume(totalWeightLb, totalVolume);

        logger.debug("Recommended container: {} for weight: {} lb, volume: {} cu.in.",
                recommended, totalWeightLb, totalVolume);

        return recommended;
    }

    /**
     * Validate if item can fit in container
     */
    public boolean canItemFitInContainer(PackingInstruction item, Container container) {
        if (item == null || container == null) {
            return false;
        }

        Weight itemWeight = item.getTotalWeight();

        // Check weight capacity
        if (!container.canHold(itemWeight)) {
            logger.debug("Item {} exceeds container {} weight capacity", item.getItemSku(), container.getContainerId());
            return false;
        }

        // Check dimensional fit if dimensions available
        if (item.getItemDimensions() != null && container.getDimensions() != null) {
            if (!item.getItemDimensions().fitsInside(container.getDimensions())) {
                logger.debug("Item {} does not fit dimensionally in container {}",
                        item.getItemSku(), container.getContainerId());
                return false;
            }
        }

        return true;
    }

    /**
     * Calculate container utilization efficiency
     */
    public double calculateUtilization(Container container) {
        if (container == null) {
            return 0.0;
        }

        return container.getUtilizationPercentage();
    }

    /**
     * Determine if items should be split across multiple containers
     */
    public boolean shouldSplitItems(List<PackingInstruction> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }

        double totalWeightLb = items.stream()
                .mapToDouble(i -> i.getTotalWeight().toPounds())
                .sum();

        // Split if exceeds max container weight
        if (totalWeightLb > MAX_CONTAINER_WEIGHT_LB) {
            logger.info("Items should be split: total weight {} lb exceeds max {} lb",
                    totalWeightLb, MAX_CONTAINER_WEIGHT_LB);
            return true;
        }

        // Split if exceeds largest container capacity
        ContainerType largest = ContainerType.EXTRA_LARGE_BOX;
        if (totalWeightLb > largest.getMaxWeightLb()) {
            logger.info("Items should be split: total weight {} lb exceeds largest container {} lb",
                    totalWeightLb, largest.getMaxWeightLb());
            return true;
        }

        return false;
    }

    /**
     * Suggest number of containers needed
     */
    public int suggestContainerCount(List<PackingInstruction> items) {
        if (items == null || items.isEmpty()) {
            return 1;
        }

        double totalWeightLb = items.stream()
                .mapToDouble(i -> i.getTotalWeight().toPounds())
                .sum();

        // Use largest box as reference
        double maxBoxWeight = ContainerType.EXTRA_LARGE_BOX.getMaxWeightLb();

        // Calculate minimum containers needed based on weight
        int containerCount = (int) Math.ceil(totalWeightLb / maxBoxWeight);

        return Math.max(1, containerCount);
    }

    /**
     * Select optimal container for new item
     */
    public Container selectBestContainer(PackingInstruction item, List<Container> existingContainers) {
        if (item == null) {
            return null;
        }

        // Try to find existing open container that can hold the item
        for (Container container : existingContainers) {
            if (container.getStatus() == Container.ContainerStatus.OPEN &&
                canItemFitInContainer(item, container)) {
                logger.debug("Selected existing container {} for item {}",
                        container.getContainerId(), item.getItemSku());
                return container;
            }
        }

        // No suitable existing container - will need new one
        logger.debug("No suitable existing container for item {}, new container recommended",
                item.getItemSku());
        return null;
    }

    // Private helper methods

    private ContainerType selectContainerByWeightAndVolume(double weightLb, double volumeCuIn) {
        // Check from smallest to largest
        if (weightLb <= ContainerType.SMALL_BOX.getMaxWeightLb() &&
            volumeCuIn <= ContainerType.SMALL_BOX.getVolumeCubicInches()) {
            return ContainerType.SMALL_BOX;
        }

        if (weightLb <= ContainerType.MEDIUM_BOX.getMaxWeightLb() &&
            volumeCuIn <= ContainerType.MEDIUM_BOX.getVolumeCubicInches()) {
            return ContainerType.MEDIUM_BOX;
        }

        if (weightLb <= ContainerType.LARGE_BOX.getMaxWeightLb() &&
            volumeCuIn <= ContainerType.LARGE_BOX.getVolumeCubicInches()) {
            return ContainerType.LARGE_BOX;
        }

        if (weightLb <= ContainerType.EXTRA_LARGE_BOX.getMaxWeightLb() &&
            volumeCuIn <= ContainerType.EXTRA_LARGE_BOX.getVolumeCubicInches()) {
            return ContainerType.EXTRA_LARGE_BOX;
        }

        // If too heavy for boxes, use pallet
        if (weightLb <= ContainerType.PALLET.getMaxWeightLb()) {
            return ContainerType.PALLET;
        }

        // Custom container needed
        return ContainerType.CUSTOM;
    }
}
