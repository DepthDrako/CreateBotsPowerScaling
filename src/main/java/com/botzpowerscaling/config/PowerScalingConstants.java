package com.botzpowerscaling.config;

/**
 * Constants for power level calculation and calibration.
 *
 * CALIBRATION BASIS:
 * - Full Netherite armor (no enchants, no effects) = Power Level 60
 * - Netherite armor provides: 20 armor, 12 toughness
 * - Base player has: 20 max health, 1.0 attack damage (fist), 0 armor, 0 toughness
 *
 * FORMULA:
 * Power = (Health * HEALTH_WEIGHT) + (Attack * ATTACK_WEIGHT) + (Defense * DEFENSE_WEIGHT) + (Armor * ARMOR_WEIGHT) + BASE_POWER
 *
 * Where:
 * - Health = max health (not current health)
 * - Attack = base attack damage + weapon damage + strength effect bonus
 * - Defense = armor toughness + (resistance level * RESISTANCE_MULTIPLIER) + protection bonus
 * - Armor = total armor points
 *
 * CALIBRATION CALCULATION:
 * Full Netherite player (no weapon):
 * - Health: 20.0 (base max health)
 * - Attack: 1.0 (base fist damage)
 * - Defense: 12.0 (toughness)
 * - Armor: 20.0
 *
 * We want: (20.0 * HW) + (1.0 * AW) + (12.0 * DW) + (20.0 * ArW) + BASE = 60
 *
 * Using weights: HW=0.5, AW=1.5, DW=1.25, ArW=1.25, BASE=5
 * Result: (20*0.5) + (1*1.5) + (12*1.25) + (20*1.25) + 5 = 10 + 1.5 + 15 + 25 + 5 = 56.5 ≈ 60
 *
 * Adjusted: HW=0.5, AW=1.5, DW=1.0, ArW=1.25, BASE=8
 * Result: (20*0.5) + (1*1.5) + (12*1.0) + (20*1.25) + 8 = 10 + 1.5 + 12 + 25 + 8 = 56.5
 *
 * Final: HW=0.5, AW=1.5, DW=1.0, ArW=1.25, BASE=11.5
 * Result: (20*0.5) + (1*1.5) + (12*1.0) + (20*1.25) + 11.5 = 10 + 1.5 + 12 + 25 + 11.5 = 60 ✓
 */
public final class PowerScalingConstants {

    private PowerScalingConstants() {} // Prevent instantiation

    // ==================== POWER FORMULA WEIGHTS ====================

    /**
     * Weight multiplier for Health (max health) in power calculation.
     * Base player has 20 health (10 hearts).
     */
    public static final double HEALTH_WEIGHT = 0.5;

    /**
     * Weight multiplier for Attack stat in power calculation.
     * Higher values make attack more impactful on total power.
     */
    public static final double ATTACK_WEIGHT = 1.5;

    /**
     * Weight multiplier for Defense stat in power calculation.
     * Defense = toughness + resistance bonus + protection bonus.
     */
    public static final double DEFENSE_WEIGHT = 1.0;

    /**
     * Weight multiplier for Armor stat in power calculation.
     */
    public static final double ARMOR_WEIGHT = 1.25;

    /**
     * Base power level added to all entities.
     * Represents inherent "being alive" value.
     */
    public static final double BASE_POWER = 11.5;

    // ==================== STAT CALCULATION MODIFIERS ====================

    /**
     * Multiplier for Resistance potion effect contribution to Defense.
     * Each level of Resistance adds this much to Defense stat.
     * Resistance I = 20% damage reduction, so we treat it as +4 effective toughness per level.
     */
    public static final double RESISTANCE_DEFENSE_PER_LEVEL = 4.0;

    /**
     * Attack damage bonus per level of Strength effect.
     * Vanilla: Strength I = +3 damage, Strength II = +6 damage
     */
    public static final double STRENGTH_ATTACK_PER_LEVEL = 3.0;

    /**
     * Attack damage reduction per level of Weakness effect.
     * Vanilla: Weakness I = -4 damage
     */
    public static final double WEAKNESS_ATTACK_PER_LEVEL = 4.0;

    /**
     * Protection enchantment contribution to Defense.
     * Each level of Protection (across all armor) adds this to Defense.
     * Max Protection is 16 levels total (4 pieces * 4 levels).
     */
    public static final double PROTECTION_DEFENSE_PER_LEVEL = 0.5;

    /**
     * Health Boost effect contribution to max health.
     * Each level adds 4 health (2 hearts).
     */
    public static final double HEALTH_BOOST_PER_LEVEL = 4.0;

    // ==================== SCANNER SETTINGS ====================

    /**
     * Maximum distance (in blocks) to scan for entities.
     */
    public static final double SCAN_RANGE = 16.0;

    /**
     * Number of ticks to cache entity stats before recalculating.
     * 20 ticks = 1 second. Using 10 ticks = 0.5 seconds for responsive feel.
     */
    public static final int CACHE_DURATION_TICKS = 10;

    /**
     * Fade-in duration for the overlay (in render frames).
     * Matches Create's goggle overlay fade behavior.
     */
    public static final int OVERLAY_FADE_TICKS = 24;

    // ==================== DISPLAY SETTINGS ====================

    /**
     * Horizontal offset from screen center for overlay position.
     */
    public static final int OVERLAY_OFFSET_X = 20;

    /**
     * Vertical offset from screen center for overlay position.
     */
    public static final int OVERLAY_OFFSET_Y = 0;

    // ==================== REFERENCE VALUES (for documentation) ====================

    /**
     * Reference: Base player max health
     */
    public static final double BASE_PLAYER_HEALTH = 20.0;

    /**
     * Reference: Full Netherite armor stats
     * - Helmet: 3 armor, 3 toughness
     * - Chestplate: 8 armor, 3 toughness
     * - Leggings: 6 armor, 3 toughness
     * - Boots: 3 armor, 3 toughness
     * - Total: 20 armor, 12 toughness
     */
    public static final double NETHERITE_FULL_ARMOR = 20.0;
    public static final double NETHERITE_FULL_TOUGHNESS = 12.0;

    /**
     * Reference: Netherite sword attack damage
     * Base: 8.0 damage
     */
    public static final double NETHERITE_SWORD_DAMAGE = 8.0;

    /**
     * Reference: Expected power level for calibration target
     */
    public static final double CALIBRATION_TARGET_POWER = 60.0;
}
