package com.botzpowerscaling.item;

/**
 * Defines the three tiers of Power Goggles and what features each unlocks.
 *
 * Tier 1 (Basic): Power Level only, 8 block range
 * Tier 2 (Advanced): Power Level + ATK/DEF/ARM/MAG stats, 16 block range
 * Tier 3 (Master): Everything including potion effects, mob level, and equipment, 32 block range
 */
public enum GogglesTier {

    NONE(0, 0, false, false, false, false, false),
    BASIC(1, 8, true, false, false, false, false),       // Tier 1: Power Level only, 8 blocks
    ADVANCED(2, 16, true, true, false, false, false),    // Tier 2: + Stats, 16 blocks
    MASTER(3, 32, true, true, true, true, true);         // Tier 3: + Everything, 32 blocks

    private final int level;
    private final double scanRange;
    private final boolean showPowerLevel;
    private final boolean showStats;
    private final boolean showPotionEffects;
    private final boolean showMobLevel;
    private final boolean showEquipment;

    GogglesTier(int level, double scanRange, boolean showPowerLevel, boolean showStats,
                boolean showPotionEffects, boolean showMobLevel, boolean showEquipment) {
        this.level = level;
        this.scanRange = scanRange;
        this.showPowerLevel = showPowerLevel;
        this.showStats = showStats;
        this.showPotionEffects = showPotionEffects;
        this.showMobLevel = showMobLevel;
        this.showEquipment = showEquipment;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Gets the scan range for this tier in blocks.
     * - Tier 1 (Basic): 8 blocks
     * - Tier 2 (Advanced): 16 blocks
     * - Tier 3 (Master): 32 blocks
     */
    public double getScanRange() {
        return scanRange;
    }

    /**
     * Whether this tier shows the Power Level.
     * Available at Tier 1+
     */
    public boolean canShowRuneLevel() {
        return showPowerLevel;
    }

    /**
     * Whether this tier shows equipped armor and weapons.
     * Available at Tier 3 only
     */
    public boolean canShowEquipment() {
        return showEquipment;
    }

    /**
     * Whether this tier shows combat stats (ATK, DEF, ARM, MAG).
     * Available at Tier 2+
     */
    public boolean canShowStats() {
        return showStats;
    }

    /**
     * Whether this tier shows potion effect icons.
     * Available at Tier 3 only
     */
    public boolean canShowPotionEffects() {
        return showPotionEffects;
    }

    /**
     * Whether this tier shows the mob's Auto-Leveling level.
     * Available at Tier 3 only
     */
    public boolean canShowMobLevel() {
        return showMobLevel;
    }

    /**
     * Whether this tier shows the health bar.
     * Available at Tier 2+ (health is a stat)
     */
    public boolean canShowHealthBar() {
        return showStats;
    }

    /**
     * Check if this tier is at least the given tier level.
     */
    public boolean isAtLeast(GogglesTier other) {
        return this.level >= other.level;
    }
}
