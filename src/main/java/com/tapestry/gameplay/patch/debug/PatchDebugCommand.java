package com.tapestry.gameplay.patch.debug;

import com.tapestry.gameplay.patch.*;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Debug command for inspecting applied patches to a specific target.
 * 
 * <p>This command provides detailed information about all patch sets applied to
 * a given target identifier, including mod IDs, priorities, operation types, and
 * statistics. It is useful for debugging modification conflicts and ordering issues.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * PatchDebugCommand command = new PatchDebugCommand(patchPlan);
 * String output = command.execute(new Identifier("minecraft:villager/farmer"));
 * System.out.println(output);
 * }</pre>
 * 
 * <p>Example output:</p>
 * <pre>
 * Target: minecraft:villager/farmer (TradeTable)
 * 
 * Applied 2 patch sets:
 * 
 * 1. mymod:farmer_trades (priority: 0)
 *    - AddTradeOperation: AddTrade[input=minecraft:wheat, output=minecraft:emerald, level=1]
 *    - RemoveTradeOperation: RemoveTrade[filter=inputItem=minecraft:cod]
 * 
 * 2. othermod:trade_tweaks (priority: 100)
 *    - ReplaceTradeInputOperation: ReplaceInput[old=minecraft:wheat, new=minecraft:hay_block]
 * 
 * Statistics:
 * - Total operations: 3
 * - Total patch sets: 2
 * </pre>
 * 
 * @see PatchPlan
 * @see PatchSet
 * @see PatchOperation
 */
public class PatchDebugCommand {
    private final PatchPlan plan;
    
    /**
     * Creates a new debug command with the given patch plan.
     * 
     * @param plan The compiled patch plan to inspect
     * @throws NullPointerException if plan is null
     */
    public PatchDebugCommand(PatchPlan plan) {
        this.plan = Objects.requireNonNull(plan, "PatchPlan cannot be null");
    }
    
    /**
     * Executes the debug command for the given target identifier.
     * 
     * <p>This method searches the patch plan for all targets matching the given
     * identifier and formats detailed information about the patches applied to
     * that target.</p>
     * 
     * <p>If no patches are found for the target, returns a message indicating
     * that no patches were found.</p>
     * 
     * @param targetId The identifier of the target to inspect
     * @return A formatted string containing patch information
     * @throws NullPointerException if targetId is null
     */
    public String execute(Identifier targetId) {
        Objects.requireNonNull(targetId, "Target identifier cannot be null");
        
        // Find target in plan by searching all targets
        PatchTarget<?> matchingTarget = findTarget(targetId);
        
        if (matchingTarget == null) {
            return formatNoPatches(targetId);
        }
        
        // Get patch sets for the target
        @SuppressWarnings("unchecked")
        List<PatchSet<?>> patches = (List<PatchSet<?>>) (List<?>) plan.getPatchesFor(matchingTarget);
        
        if (patches.isEmpty()) {
            return formatNoPatches(targetId);
        }
        
        // Format output
        return formatPatchInfo(matchingTarget, patches);
    }
    
    /**
     * Searches the patch plan for a target with the given identifier.
     * 
     * <p>This method iterates through all targets in the patch plan to find one
     * matching the given identifier. Returns null if no match is found.</p>
     * 
     * @param targetId The identifier to search for
     * @return The matching PatchTarget, or null if not found
     */
    private PatchTarget<?> findTarget(Identifier targetId) {
        // Search through all targets in the plan
        for (PatchTarget<?> target : plan.getAllTargets().keySet()) {
            if (target.id().equals(targetId)) {
                return target;
            }
        }
        
        return null;
    }
    
    /**
     * Formats a message indicating no patches were found for the target.
     * 
     * @param targetId The target identifier
     * @return A formatted message
     */
    private String formatNoPatches(Identifier targetId) {
        return String.format("No patches found for target: %s", targetId);
    }
    
    /**
     * Formats detailed information about patches applied to a target.
     * 
     * <p>The output includes:</p>
     * <ul>
     *   <li>Target identifier and type</li>
     *   <li>Number of patch sets applied</li>
     *   <li>For each patch set: mod ID, priority, and operations</li>
     *   <li>For each operation: operation type and debug ID (if available)</li>
     *   <li>Summary statistics</li>
     * </ul>
     * 
     * @param target The patch target
     * @param patches The list of patch sets applied to the target
     * @return A formatted string with patch information
     */
    private String formatPatchInfo(PatchTarget<?> target, List<PatchSet<?>> patches) {
        StringBuilder output = new StringBuilder();
        
        // Header
        output.append(String.format("Target: %s (%s)\n\n", 
            target.id(), 
            target.type().getSimpleName()));
        
        output.append(String.format("Applied %d patch set%s:\n\n", 
            patches.size(),
            patches.size() == 1 ? "" : "s"));
        
        // Patch sets
        int totalOperations = 0;
        for (int i = 0; i < patches.size(); i++) {
            PatchSet<?> patchSet = patches.get(i);
            
            output.append(String.format("%d. %s (priority: %d)\n", 
                i + 1, 
                patchSet.modId(), 
                patchSet.priority()));
            
            // Operations
            for (PatchOperation<?> operation : patchSet.operations()) {
                String operationType = operation.getClass().getSimpleName();
                String debugInfo = operation.getDebugId()
                    .map(id -> ": " + id)
                    .orElse("");
                
                output.append(String.format("   - %s%s\n", 
                    operationType,
                    debugInfo));
                
                totalOperations++;
            }
            
            output.append("\n");
        }
        
        // Statistics
        output.append("Statistics:\n");
        output.append(String.format("- Total operations: %d\n", totalOperations));
        output.append(String.format("- Total patch sets: %d\n", patches.size()));
        
        return output.toString();
    }
}
