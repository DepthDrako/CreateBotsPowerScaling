package com.botzpowerscaling.scanner;

import com.botzpowerscaling.config.PowerScalingConfig;
import com.botzpowerscaling.config.PowerScalingConstants;
import com.botzpowerscaling.integration.AutoLevelingIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Calculates combat stats for living entities.
 *
 * STAT DEFINITIONS:
 *
 * HEALTH:
 * - Base: Entity's MAX_HEALTH attribute
 * - This is the maximum possible health, not current health
 * - Includes Health Boost effect bonus
 *
 * ATTACK:
 * - Base: Entity's ATTACK_DAMAGE attribute (includes held weapon)
 * - Plus: Strength effect bonus (+3 per level)
 * - Minus: Weakness effect penalty (-4 per level)
 * - Plus: Configured attack attributes from other mods
 * - Minimum: 0
 *
 * DEFENSE:
 * - Base: Entity's ARMOR_TOUGHNESS attribute
 * - Plus: Knockback Resistance * 10 (0-1 scaled to 0-10)
 * - Plus: Total Protection enchantment levels * 0.5
 * - Plus: Resistance effect bonus (+4 per level)
 *
 * ARMOR:
 * - Direct: Entity's ARMOR attribute value
 *
 * MAGIC:
 * - Calculated from configured magic attributes
 * - Default: 0 (unless magic mods are configured)
 *
 * POWER LEVEL:
 * - Formula: (Health * healthWeight) + (Attack * attackWeight) + (Defense * defenseWeight)
 *            + (Armor * armorWeight) + (Magic * magicWeight) + basePowerOffset
 * - Weights are configurable
 * - Calibrated so Full Netherite (no weapon/enchants/effects) = 60 with default settings
 */
public final class StatsCalculator {

    private StatsCalculator() {} // Prevent instantiation

    /**
     * Calculates all stats for a living entity, including potion effect bonuses.
     *
     * @param entity The entity to scan
     * @return EntityStats containing all calculated values and bonuses
     */
    public static EntityStats calculate(LivingEntity entity) {
        if (entity == null) {
            return EntityStats.empty();
        }

        double currentHealth = entity.getHealth();
        double maxHealth = calculateMaxHealth(entity);

        // Calculate attack - base from attribute, bonus from potion effects
        double baseAttack = calculateBaseAttack(entity);
        double attackBonus = calculateAttackBonus(entity);
        double attack = Math.max(0, baseAttack + attackBonus);

        // Calculate defense and track bonus separately
        double baseDefense = calculateBaseDefense(entity);
        double defenseBonus = calculateDefenseBonus(entity);
        double defense = Math.max(0, baseDefense + defenseBonus);

        double armor = calculateArmor(entity);

        // Calculate magic stat
        double baseMagic = calculateMagic(entity);
        double magicBonus = calculateMagicBonus(entity);
        double magic = Math.max(0, baseMagic + magicBonus);

        // Get weights from config
        double healthWeight = getHealthWeight();
        double attackWeight = getAttackWeight();
        double defenseWeight = getDefenseWeight();
        double armorWeight = getArmorWeight();
        double magicWeight = getMagicWeight();
        double basePower = getBasePowerOffset();

        // Calculate power levels
        double basePowerLevel = calculatePowerLevel(maxHealth, baseAttack, baseDefense, armor, baseMagic,
                healthWeight, attackWeight, defenseWeight, armorWeight, magicWeight, basePower);
        double totalPowerLevel = calculatePowerLevel(maxHealth, attack, defense, armor, magic,
                healthWeight, attackWeight, defenseWeight, armorWeight, magicWeight, basePower);
        double powerBonus = totalPowerLevel - basePowerLevel;

        String name = entity.getName().getString();

        // Get mob level from Auto-Leveling if available
        int mobLevel = AutoLevelingIntegration.getMobLevel(entity);

        return new EntityStats(currentHealth, maxHealth, attack, defense, armor, magic, totalPowerLevel, name,
                attackBonus, defenseBonus, magicBonus, powerBonus, mobLevel);
    }

    // Config getters with fallbacks to constants
    private static double getHealthWeight() {
        try {
            return PowerScalingConfig.COMMON.healthWeight.get();
        } catch (Exception e) {
            return PowerScalingConstants.HEALTH_WEIGHT;
        }
    }

    private static double getAttackWeight() {
        try {
            return PowerScalingConfig.COMMON.attackWeight.get();
        } catch (Exception e) {
            return PowerScalingConstants.ATTACK_WEIGHT;
        }
    }

    private static double getDefenseWeight() {
        try {
            return PowerScalingConfig.COMMON.defenseWeight.get();
        } catch (Exception e) {
            return PowerScalingConstants.DEFENSE_WEIGHT;
        }
    }

    private static double getArmorWeight() {
        try {
            return PowerScalingConfig.COMMON.armorWeight.get();
        } catch (Exception e) {
            return PowerScalingConstants.ARMOR_WEIGHT;
        }
    }

    private static double getMagicWeight() {
        try {
            return PowerScalingConfig.COMMON.magicWeight.get();
        } catch (Exception e) {
            return 1.5; // Default magic weight
        }
    }

    private static double getBasePowerOffset() {
        try {
            return PowerScalingConfig.COMMON.basePowerOffset.get();
        } catch (Exception e) {
            return PowerScalingConstants.BASE_POWER;
        }
    }

    /**
     * Calculates the max health stat.
     */
    private static double calculateMaxHealth(LivingEntity entity) {
        if (entity.getAttributes().hasAttribute(Attributes.MAX_HEALTH)) {
            return entity.getAttributeValue(Attributes.MAX_HEALTH);
        }
        return 20.0;
    }

    /**
     * Calculates base Attack stat from attribute (without potion effects).
     * Note: For most mobs, the ATTACK_DAMAGE attribute doesn't include potion bonuses.
     * Those are applied during damage calculation, not stored in the attribute.
     */
    private static double calculateBaseAttack(LivingEntity entity) {
        double attack = 1.0;

        if (entity.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            attack = entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
        }

        // Add configured attack attributes
        attack += getAttributeValueFromConfig(entity, getAttackAttributes());

        return Math.max(0, attack);
    }

    /**
     * Calculates the attack bonus/penalty from potion effects.
     * Positive for Strength, negative for Weakness.
     * For the local player, reads directly from entity.
     * For other entities, uses cached data from server.
     */
    private static double calculateAttackBonus(LivingEntity entity) {
        double bonus = 0.0;

        // Check if this is the local player - we can read their effects directly
        Player localPlayer = Minecraft.getInstance().player;
        boolean isLocalPlayer = localPlayer != null && entity.getId() == localPlayer.getId();

        if (isLocalPlayer) {
            // Local player - read effects directly
            var strengthEffect = entity.getEffect(MobEffects.DAMAGE_BOOST);
            if (strengthEffect != null) {
                int strengthLevel = strengthEffect.getAmplifier() + 1;
                bonus += strengthLevel * PowerScalingConstants.STRENGTH_ATTACK_PER_LEVEL;
            }

            var weaknessEffect = entity.getEffect(MobEffects.WEAKNESS);
            if (weaknessEffect != null) {
                int weaknessLevel = weaknessEffect.getAmplifier() + 1;
                bonus -= weaknessLevel * PowerScalingConstants.WEAKNESS_ATTACK_PER_LEVEL;
            }
        } else {
            // Other entities - use cached data from server
            EffectDataCache.EffectData effectData = EffectDataCache.getEffectData(entity.getId());
            if (effectData != null) {
                if (effectData.hasStrength()) {
                    bonus += effectData.strengthLevel() * PowerScalingConstants.STRENGTH_ATTACK_PER_LEVEL;
                }
                if (effectData.hasWeakness()) {
                    bonus -= effectData.weaknessLevel() * PowerScalingConstants.WEAKNESS_ATTACK_PER_LEVEL;
                }
            }
        }

        return bonus;
    }

    /**
     * Calculates base Defense stat (without potion effects).
     * Includes armor toughness, knockback resistance, and protection enchantments.
     */
    private static double calculateBaseDefense(LivingEntity entity) {
        double defense = 0.0;

        // Armor toughness contributes to defense
        if (entity.getAttributes().hasAttribute(Attributes.ARMOR_TOUGHNESS)) {
            defense += entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        }

        // Knockback resistance (0-1) scaled to 0-10 defense
        // Iron Golem has 1.0 = +10 DEF, Ravager has 0.5 = +5 DEF
        if (entity.getAttributes().hasAttribute(Attributes.KNOCKBACK_RESISTANCE)) {
            double knockbackResist = entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            defense += knockbackResist * 10.0;
        }

        defense += calculateProtectionBonus(entity);

        // Add configured defense attributes
        defense += getAttributeValueFromConfig(entity, getDefenseAttributes());

        return Math.max(0, defense);
    }

    /**
     * Calculates the defense bonus from potion effects.
     * For the local player, reads directly from entity.
     * For other entities, uses cached data from server.
     */
    private static double calculateDefenseBonus(LivingEntity entity) {
        double bonus = 0.0;

        // Check if this is the local player - we can read their effects directly
        Player localPlayer = Minecraft.getInstance().player;
        boolean isLocalPlayer = localPlayer != null && entity.getId() == localPlayer.getId();

        if (isLocalPlayer) {
            // Local player - read effects directly
            var resistanceEffect = entity.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (resistanceEffect != null) {
                int resistanceLevel = resistanceEffect.getAmplifier() + 1;
                bonus += resistanceLevel * PowerScalingConstants.RESISTANCE_DEFENSE_PER_LEVEL;
            }
        } else {
            // Other entities - use cached data from server
            EffectDataCache.EffectData effectData = EffectDataCache.getEffectData(entity.getId());
            if (effectData != null) {
                if (effectData.hasResistance()) {
                    bonus += effectData.resistanceLevel() * PowerScalingConstants.RESISTANCE_DEFENSE_PER_LEVEL;
                }
            }
        }

        return bonus;
    }

    /**
     * Calculates total Protection enchantment contribution to defense.
     */
    private static double calculateProtectionBonus(LivingEntity entity) {
        int totalProtection = 0;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {

            ItemStack armor = entity.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                int protLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, armor);
                totalProtection += protLevel;

                totalProtection += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, armor) / 2;
                totalProtection += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLAST_PROTECTION, armor) / 2;
                totalProtection += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, armor) / 2;
            }
        }

        return totalProtection * PowerScalingConstants.PROTECTION_DEFENSE_PER_LEVEL;
    }

    /**
     * Calculates the Armor stat.
     */
    private static double calculateArmor(LivingEntity entity) {
        if (entity.getAttributes().hasAttribute(Attributes.ARMOR)) {
            return entity.getAttributeValue(Attributes.ARMOR);
        }
        return 0.0;
    }

    /**
     * Calculates the Magic stat from configured attributes.
     */
    private static double calculateMagic(LivingEntity entity) {
        return getAttributeValueFromConfig(entity, getMagicAttributes());
    }

    /**
     * Calculates magic bonus from effects (placeholder for future magic effect support).
     */
    private static double calculateMagicBonus(LivingEntity entity) {
        // Currently no magic potion effects supported
        // This can be extended to support modded magic effects
        return 0.0;
    }

    /**
     * Gets the sum of attribute values from a list of attribute IDs.
     */
    private static double getAttributeValueFromConfig(LivingEntity entity, List<? extends String> attributeIds) {
        double total = 0.0;

        for (String attrId : attributeIds) {
            try {
                ResourceLocation loc = new ResourceLocation(attrId);
                Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(loc);
                if (attr != null && entity.getAttributes().hasAttribute(attr)) {
                    total += entity.getAttributeValue(attr);
                }
            } catch (Exception e) {
                // Invalid attribute ID, skip
            }
        }

        return total;
    }

    // Config attribute list getters
    private static List<? extends String> getAttackAttributes() {
        try {
            return PowerScalingConfig.COMMON.attackAttributes.get();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<? extends String> getDefenseAttributes() {
        try {
            return PowerScalingConfig.COMMON.defenseAttributes.get();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<? extends String> getMagicAttributes() {
        try {
            return PowerScalingConfig.COMMON.magicAttributes.get();
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Calculates Power Level from component stats.
     */
    private static double calculatePowerLevel(double health, double attack, double defense, double armor, double magic,
                                               double healthWeight, double attackWeight, double defenseWeight,
                                               double armorWeight, double magicWeight, double basePower) {
        return (health * healthWeight)
                + (attack * attackWeight)
                + (defense * defenseWeight)
                + (armor * armorWeight)
                + (magic * magicWeight)
                + basePower;
    }
}
