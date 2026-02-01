package com.botzpowerscaling.integration;

import com.botzpowerscaling.config.PowerScalingConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

/**
 * Integration with the Auto-Leveling mod by Daripher.
 * Detects mob levels stored in NBT data.
 */
public class AutoLevelingIntegration {

    private static final String AUTO_LEVELING_MOD_ID = "autoleveling";
    private static final String LEVEL_TAG = "LEVEL";

    private static Boolean modLoaded = null;

    /**
     * Checks if the Auto-Leveling mod is installed.
     */
    public static boolean isModLoaded() {
        if (modLoaded == null) {
            modLoaded = ModList.get().isLoaded(AUTO_LEVELING_MOD_ID);
        }
        return modLoaded;
    }

    /**
     * Checks if integration is enabled in config and mod is loaded.
     */
    public static boolean isIntegrationEnabled() {
        return isModLoaded() && PowerScalingConfig.COMMON.autoLevelingIntegration.get();
    }

    /**
     * Checks if the entity has a level assigned by Auto-Leveling.
     */
    public static boolean hasLevel(LivingEntity entity) {
        if (!isIntegrationEnabled()) {
            return false;
        }

        try {
            CompoundTag persistentData = entity.getPersistentData();
            return persistentData.contains(LEVEL_TAG);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the mob's level from Auto-Leveling.
     * Returns 0 if no level is set or mod is not loaded.
     */
    public static int getMobLevel(LivingEntity entity) {
        if (!isIntegrationEnabled()) {
            return 0;
        }

        try {
            CompoundTag persistentData = entity.getPersistentData();
            if (persistentData.contains(LEVEL_TAG)) {
                return persistentData.getInt(LEVEL_TAG);
            }
        } catch (Exception e) {
            // Silently fail if we can't read the level
        }

        return 0;
    }

    /**
     * Checks if we should show mob levels in the UI.
     */
    public static boolean shouldShowMobLevel() {
        return isIntegrationEnabled() && PowerScalingConfig.COMMON.showMobLevel.get();
    }
}
