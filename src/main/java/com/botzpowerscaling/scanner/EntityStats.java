package com.botzpowerscaling.scanner;

/**
 * Immutable record holding calculated stats for a scanned entity.
 * All values are the final computed numbers ready for display.
 * Includes bonus tracking for potion effects and Auto-Leveling integration.
 */
public record EntityStats(
        double currentHealth,
        double maxHealth,
        double attack,
        double defense,
        double armor,
        double magic,
        double powerLevel,
        String entityName,
        // Bonus values from potion effects
        double attackBonus,
        double defenseBonus,
        double magicBonus,
        double powerBonus,
        // Auto-Leveling integration
        int mobLevel
) {
    /**
     * Creates a default/empty stats object for when no entity is targeted.
     */
    public static EntityStats empty() {
        return new EntityStats(0, 0, 0, 0, 0, 0, 0, "", 0, 0, 0, 0, 0);
    }

    /**
     * @return true if this represents valid entity stats (not empty)
     */
    public boolean isValid() {
        return !entityName.isEmpty();
    }

    /**
     * @return Current health formatted as integer for display
     */
    public int getDisplayCurrentHealth() {
        return (int) Math.round(currentHealth);
    }

    /**
     * @return Max health formatted as integer for display
     */
    public int getDisplayMaxHealth() {
        return (int) Math.round(maxHealth);
    }

    /**
     * @return Health display string in format "current/max"
     */
    public String getDisplayHealthString() {
        return getDisplayCurrentHealth() + "/" + getDisplayMaxHealth();
    }

    /**
     * @return Base attack (without bonus) formatted as integer for display
     */
    public int getDisplayBaseAttack() {
        return (int) Math.round(attack - attackBonus);
    }

    /**
     * @return Attack stat formatted as integer for display
     */
    public int getDisplayAttack() {
        return (int) Math.round(attack);
    }

    /**
     * @return Attack bonus from effects formatted as integer
     */
    public int getDisplayAttackBonus() {
        return (int) Math.round(attackBonus);
    }

    /**
     * @return true if there's an attack bonus active
     */
    public boolean hasAttackBonus() {
        return Math.abs(attackBonus) >= 0.5;
    }

    /**
     * @return Base defense (without bonus) formatted as integer for display
     */
    public int getDisplayBaseDefense() {
        return (int) Math.round(defense - defenseBonus);
    }

    /**
     * @return Defense stat formatted as integer for display
     */
    public int getDisplayDefense() {
        return (int) Math.round(defense);
    }

    /**
     * @return Defense bonus from effects formatted as integer
     */
    public int getDisplayDefenseBonus() {
        return (int) Math.round(defenseBonus);
    }

    /**
     * @return true if there's a defense bonus active
     */
    public boolean hasDefenseBonus() {
        return Math.abs(defenseBonus) >= 0.5;
    }

    /**
     * @return Armor stat formatted as integer for display
     */
    public int getDisplayArmor() {
        return (int) Math.round(armor);
    }

    /**
     * @return Base magic (without bonus) formatted as integer for display
     */
    public int getDisplayBaseMagic() {
        return (int) Math.round(magic - magicBonus);
    }

    /**
     * @return Magic stat formatted as integer for display
     */
    public int getDisplayMagic() {
        return (int) Math.round(magic);
    }

    /**
     * @return Magic bonus from effects formatted as integer
     */
    public int getDisplayMagicBonus() {
        return (int) Math.round(magicBonus);
    }

    /**
     * @return true if there's a magic bonus active
     */
    public boolean hasMagicBonus() {
        return Math.abs(magicBonus) >= 0.5;
    }

    /**
     * @return true if magic stat is greater than 0
     */
    public boolean hasMagic() {
        return magic >= 0.5;
    }

    /**
     * @return true if defense stat is greater than 0
     */
    public boolean hasDefense() {
        return defense >= 0.5;
    }

    /**
     * @return true if armor stat is greater than 0
     */
    public boolean hasArmor() {
        return armor >= 0.5;
    }

    /**
     * @return Base power level (without bonus) formatted as integer for display
     */
    public int getDisplayBasePowerLevel() {
        return (int) Math.round(powerLevel - powerBonus);
    }

    /**
     * @return Power level formatted as integer for display
     */
    public int getDisplayPowerLevel() {
        return (int) Math.round(powerLevel);
    }

    /**
     * @return Power bonus from effects formatted as integer
     */
    public int getDisplayPowerBonus() {
        return (int) Math.round(powerBonus);
    }

    /**
     * @return true if there's a power bonus active
     */
    public boolean hasPowerBonus() {
        return Math.abs(powerBonus) >= 0.5;
    }

    /**
     * @return The mob's Auto-Leveling level, or 0 if not leveled
     */
    public int getMobLevel() {
        return mobLevel;
    }

    /**
     * @return true if the mob has an Auto-Leveling level
     */
    public boolean hasMobLevel() {
        return mobLevel > 0;
    }
}
