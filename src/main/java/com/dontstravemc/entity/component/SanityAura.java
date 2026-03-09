package com.dontstravemc.entity.component;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.function.BiFunction;

public class SanityAura<T> {
    // Default tuning value, can be adjusted or moved to a config later
    public static final double SANITY_EFFECT_RANGE = 20.0; 
    public static final double SANITY_EFFECT_RANGE_SQ = SANITY_EFFECT_RANGE * SANITY_EFFECT_RANGE;

    private final T inst;
    private double aura = 0;
    
    // Optional overrides
    private Double maxDistSq = null;
    private BiFunction<T, Entity, Double> auraFn = null;
    private TriFunction<T, Entity, Double, Double> falloffFn = null;

    public SanityAura(T inst) {
        this.inst = inst;
        // In the Lua version this adds a tag. In Java we might use an interface or capability, 
        // but for now we manage the logical component.
    }

    public void setAura(double aura) {
        this.aura = aura;
    }

    public void setAuraFn(BiFunction<T, Entity, Double> auraFn) {
        this.auraFn = auraFn;
    }

    public void setFalloffFn(TriFunction<T, Entity, Double, Double> falloffFn) {
        this.falloffFn = falloffFn;
    }

    public void setMaxDistSq(double maxDistSq) {
        this.maxDistSq = maxDistSq;
    }

    /**
     * Replicates GetBaseAura(observer) - No falloff
     */
    public double getBaseAura(Entity observer) {
        if (this.auraFn == null) {
            return this.aura;
        }
        return this.auraFn.apply(this.inst, observer);
    }

    /**
     * Replicates GetAura(observer) - With distance check and falloff
     */
    public double getAura(Entity observer) {
        // Need to calculate distance. 
        // Since T can be anything, we can't efficiently check distance unless T is known to have a position.
        // However, the Lua code uses 'observer:GetDistanceSqToInst(self.inst)'.
        // We will assume the caller handles the validity check or T is an Entity/BlockEntity with location access.
        // For general usage, we rely on the observer calculating distance itself, OR specialized methods.
        // BUT, adhering strictly to the Lua logic:
        
        double distSq = -1.0;
        
        // Try to resolve distance
        if (inst instanceof Entity entityInst) {
            distSq = observer.distanceToSqr(entityInst);
        } else if (inst instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
             distSq = observer.distanceToSqr(be.getBlockPos().getCenter());
        } else {
            // Fallback: If generic object, passed observer must handle it? 
            // In Java, we might need to pass distance explicitly if standard resolution fails, 
            // but for now we support Entity and BlockEntity.
            return 0;
        }

        double checkDist = (maxDistSq != null) ? maxDistSq : SANITY_EFFECT_RANGE_SQ;

        if (distSq <= checkDist) {
            double baseVal = (this.auraFn == null) ? this.aura : this.auraFn.apply(this.inst, observer);
            
            double divisor;
            if (this.falloffFn != null) {
                divisor = this.falloffFn.apply(this.inst, observer, distSq);
            } else {
                divisor = Math.max(1, distSq); // Default falloff is linear inverse of distance squared (roughly) or as per lua logic
                // Lua: math.max(1, distsq) implies 1/d^2 falloff intensity roughly.
            }
            
            return baseVal / divisor;
        }

        return 0;
    }
    
    // --- Serialization ---
    // Only saving basic fields. Functions/Callbacks must be re-set after load.

    public void toNBT(ValueOutput valueOutput) {
        valueOutput.putDouble("Aura", aura);
        if (maxDistSq != null) {
            valueOutput.putDouble("MaxDistSq", maxDistSq);
        }
    }

    public void fromNBT(ValueInput valueInput) {
        this.aura = valueInput.getDoubleOr("Aura", 0.0);
        // We can't easily detect if MaxDistSq was present or not with simple getDoubleOr, 
        // but for now we assume defaults or specific implementation logic.
        // If the NBT lib supports 'contains', use that. Assuming simple wrapper for now.
    }

    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
