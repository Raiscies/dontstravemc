package com.dontstravemc.sanity;

import net.minecraft.nbt.CompoundTag;

public class SanityManager {
    private static final float MAX_SANITY = 100.0f;
    private static final float MIN_SANITY = 0.0f;
    private static final float SYNC_THRESHOLD = 1.0f;
    private static final int SYNC_INTERVAL = 20; // ticks (1 second)

    private float sanity;
    private float lastSyncedValue;
    private int tickCounter;

    public SanityManager() {
        this.sanity = MAX_SANITY;
        this.lastSyncedValue = MAX_SANITY;
        this.tickCounter = 0;
    }

    public float getSanity() {
        return sanity;
    }

    public void setSanity(float value) {
        this.sanity = Math.max(MIN_SANITY, Math.min(MAX_SANITY, value));
    }

    public void addSanity(float delta) {
        setSanity(this.sanity + delta);
    }

    public float getMaxSanity() {
        return MAX_SANITY;
    }

    public void tick() {
        tickCounter++;
        // Add decay logic here if needed in the future
    }

    /**
     * Check if sanity needs to be synced to client based on threshold or interval
     * 
     * @return true if sync is needed
     */
    public boolean shouldSync() {
        boolean thresholdMet = Math.abs(sanity - lastSyncedValue) >= SYNC_THRESHOLD;
        boolean intervalMet = tickCounter >= SYNC_INTERVAL;

        if (thresholdMet || intervalMet) {
            lastSyncedValue = sanity;
            tickCounter = 0;
            return true;
        }
        return false;
    }

    public void markSynced() {
        lastSyncedValue = sanity;
        tickCounter = 0;
    }

    public void writeToNbt(CompoundTag tag) {
        tag.putFloat("Sanity", sanity);
    }

    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("Sanity")) {
            this.sanity = tag.getFloat("Sanity").orElse(MAX_SANITY);
            this.lastSyncedValue = this.sanity;
        }
    }
}
