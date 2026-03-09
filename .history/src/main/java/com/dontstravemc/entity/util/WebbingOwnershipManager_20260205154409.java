package com.dontstravemc.entity.util;

import net.minecraft.core.BlockPos;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebbingOwnershipManager {
    private static final WebbingOwnershipManager INSTANCE = new WebbingOwnershipManager();
    
    // Map: BlockPos -> Set of den UUIDs that own this webbing
    private final Map<BlockPos, Set<UUID>> ownershipMap = new ConcurrentHashMap<>();
    
    public static WebbingOwnershipManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register that a den owns a webbing block
     */
    public void addOwner(BlockPos pos, UUID denUUID) {
        ownershipMap.computeIfAbsent(pos, k -> ConcurrentHashMap.newKeySet()).add(denUUID);
    }
    
    /**
     * Check if this position has any owners besides the given denUUID
     */
    public boolean hasOtherOwners(BlockPos pos, UUID denUUID) {
        Set<UUID> owners = ownershipMap.get(pos);
        if (owners == null || owners.isEmpty()) return false;
        
        // Has other owners if: set size > 1, OR (size == 1 but that one isn't denUUID)
        return owners.size() > 1 || (owners.size() == 1 && !owners.contains(denUUID));
    }
    
    /**
     * Remove a den from ownership of a position
     */
    public void removeOwner(BlockPos pos, UUID denUUID) {
        Set<UUID> owners = ownershipMap.get(pos);
        if (owners != null) {
            owners.remove(denUUID);
            if (owners.isEmpty()) {
                ownershipMap.remove(pos); // Cleanup empty entries
            }
        }
    }
    
    /**
     * Cleanup all ownership data for a position (used when webbing is manually broken)
     */
    public void clearPosition(BlockPos pos) {
        ownershipMap.remove(pos);
    }
}
