package com.colossaldungeons.enhanced.dungeon.puzzle;

import com.colossaldungeons.enhanced.vanilla.InteractionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A puzzle where the player must place items on scales to match a target weight.
 * Items have weight values; the player places/removes items until the scale balances.
 * The solution in config contains: target weight as first element, tolerance as second element.
 */
public class WeightBalancePuzzle extends AbstractPuzzle {

    private double currentWeight;
    private double targetWeight;
    private double tolerance;
    private final Map<String, Double> itemWeights;
    private final Map<String, Integer> placedItems;

    public WeightBalancePuzzle(ResourceLocation id, PuzzleConfig config) {
        super(id, config);
        this.currentWeight = 0.0;
        this.itemWeights = new HashMap<>();
        this.placedItems = new HashMap<>();

        // Parse target weight and tolerance from solution config
        if (config.solution().size() >= 2) {
            try {
                this.targetWeight = Double.parseDouble(config.solution().get(0));
                this.tolerance = Double.parseDouble(config.solution().get(1));
            } catch (NumberFormatException e) {
                this.targetWeight = 10.0;
                this.tolerance = 0.5;
            }
        } else {
            this.targetWeight = 10.0;
            this.tolerance = 0.5;
        }

        // Default item weights (can be configured via data-driven system)
        initDefaultWeights();
    }

    private void initDefaultWeights() {
        itemWeights.put("minecraft:iron_ingot", 1.0);
        itemWeights.put("minecraft:gold_ingot", 2.0);
        itemWeights.put("minecraft:diamond", 3.0);
        itemWeights.put("minecraft:copper_ingot", 0.75);
        itemWeights.put("minecraft:netherite_ingot", 5.0);
        itemWeights.put("minecraft:cobblestone", 0.5);
        itemWeights.put("minecraft:stone", 0.5);
        itemWeights.put("minecraft:anvil", 10.0);
    }

    /**
     * Registers a custom item weight mapping.
     */
    public void setItemWeight(String itemId, double weight) {
        itemWeights.put(itemId, weight);
    }

    @Override
    protected void onActivate() {
        currentWeight = 0.0;
        placedItems.clear();
    }

    @Override
    protected void processInput(ServerPlayer player, InteractionContext context) {
        ItemStack stack = context.stack();
        if (stack.isEmpty()) {
            // Empty hand: remove last placed item (FIFO removal)
            if (!placedItems.isEmpty()) {
                // Remove one of the last added item type
                Map.Entry<String, Integer> lastEntry = null;
                for (Map.Entry<String, Integer> entry : placedItems.entrySet()) {
                    lastEntry = entry;
                }
                if (lastEntry != null && lastEntry.getValue() > 0) {
                    String itemId = lastEntry.getKey();
                    double weight = itemWeights.getOrDefault(itemId, 0.0);
                    currentWeight -= weight;
                    int newCount = lastEntry.getValue() - 1;
                    if (newCount <= 0) {
                        placedItems.remove(itemId);
                    } else {
                        placedItems.put(itemId, newCount);
                    }
                }
            }
            return;
        }

        // Place the held item on the scale
        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        double weight = itemWeights.getOrDefault(itemId, 0.25); // Default weight for unknown items

        currentWeight += weight;
        placedItems.merge(itemId, 1, Integer::sum);

        // Consume one item from the player's stack (placed on scale)
        stack.shrink(1);
    }

    @Override
    protected boolean checkSolution() {
        return PuzzleValidator.validateWeight(currentWeight, targetWeight, tolerance);
    }

    @Override
    protected void doReset() {
        currentWeight = 0.0;
        placedItems.clear();
    }

    @Override
    public void tick(ServerLevel level) {
        super.tick(level);
    }

    /**
     * Gets the current weight on the scale.
     */
    public double getCurrentWeight() {
        return currentWeight;
    }

    /**
     * Gets the target weight.
     */
    public double getTargetWeight() {
        return targetWeight;
    }

    /**
     * Gets the placed items map (item ID to count).
     */
    public Map<String, Integer> getPlacedItems() {
        return Map.copyOf(placedItems);
    }
}
