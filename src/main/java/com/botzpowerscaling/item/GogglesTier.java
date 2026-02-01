package com.botzpowerscaling.item;

/**
 * Defines the three tiers of Power Goggles and what features each unlocks.
 *
 * Tier 1 (Basic): Rune Level only
 * Tier 2 (Advanced): Rune Level + ATK/DEF/ARM/MAG stats
 * Tier 3 (Master): Everything including potion effects and mob level
 */
public enum GogglesTier {

    NONE(0, false, false, false, false),
    BASIC(1, true, false, false, false),      // Tier 1: Rune Level only
    ADVANCED(2, true, true, false, false),    // Tier 2: + Stats (ATK, DEF, ARM, MAG)
    MASTER(3, true, true, true, true);        // Tier 3: + Potion effects + Mob level

    private final int level;
    private final boolean showRuneLevel;
    private final boolean showStats;
    private final boolean showPotionEffects;
    private final boolean showMobLevel;

    GogglesTier(int level, boolean showRuneLevel, boolean showStats,
                boolean showPotionEffects, boolean showMobLevel) {
        this.level = level;
        this.showRuneLevel = showRuneLevel;
        this.showStats = showStats;
        this.showPotionEffects = showPotionEffects;
        this.showMobLevel = showMobLevel;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Whether this tier shows the Rune Level (Power Level).
     * Available at Tier 1+
     */
    public boolean canShowRuneLevel() {
        return showRuneLevel;
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
