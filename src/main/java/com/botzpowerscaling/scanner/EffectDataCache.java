package com.botzpowerscaling.scanner;

import com.botzpowerscaling.network.EntityEffectsPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for entity effect data received from the server.
 * This allows us to display potion effect bonuses and icons for entities other than the local player.
 */
public class EffectDataCache {

    /**
     * Holds effect data for entities, keyed by entity ID.
     */
    private static final Map<Integer, EffectData> CACHE = new ConcurrentHashMap<>();

    /**
     * How long cached data is valid (in milliseconds).
     */
    private static final long CACHE_EXPIRY_MS = 1000; // 1 second

    /**
     * Stores effect data for an entity.
     */
    public static void setEffectData(int entityId, int strengthLevel, int weaknessLevel, int resistanceLevel,
                                      List<EntityEffectsPacket.EffectEntry> allEffects) {
        CACHE.put(entityId, new EffectData(strengthLevel, weaknessLevel, resistanceLevel, allEffects, System.currentTimeMillis()));
    }

    /**
     * Gets cached effect data for an entity.
     * Returns null if no data or data is expired.
     */
    public static EffectData getEffectData(int entityId) {
        EffectData data = CACHE.get(entityId);
        if (data == null) {
            return null;
        }

        // Check if data is expired
        if (System.currentTimeMillis() - data.timestamp > CACHE_EXPIRY_MS) {
            CACHE.remove(entityId);
            return null;
        }

        return data;
    }

    /**
     * Clears all cached data.
     */
    public static void clear() {
        CACHE.clear();
    }

    /**
     * Removes expired entries from the cache.
     */
    public static void cleanupExpired() {
        long now = System.currentTimeMillis();
        CACHE.entrySet().removeIf(entry -> now - entry.getValue().timestamp > CACHE_EXPIRY_MS);
    }

    /**
     * Record holding effect levels, all effects list, and timestamp.
     */
    public record EffectData(
            int strengthLevel,
            int weaknessLevel,
            int resistanceLevel,
            List<EntityEffectsPacket.EffectEntry> allEffects,
            long timestamp
    ) {
        public boolean hasStrength() {
            return strengthLevel > 0;
        }

        public boolean hasWeakness() {
            return weaknessLevel > 0;
        }

        public boolean hasResistance() {
            return resistanceLevel > 0;
        }

        public boolean hasAnyEffects() {
            return allEffects != null && !allEffects.isEmpty();
        }

        public List<EntityEffectsPacket.EffectEntry> getAllEffects() {
            return allEffects != null ? allEffects : Collections.emptyList();
        }
    }
}
