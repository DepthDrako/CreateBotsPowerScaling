package com.botzpowerscaling.item;

import com.botzpowerscaling.BotzPowerScaling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * Armor materials for the three tiers of Power Goggles.
 * Each tier has slightly better stats than the last.
 */
public enum PowerGogglesMaterial implements ArmorMaterial {

    // Tier 1: Basic - leather equivalent, minimal protection
    BASIC("basic_goggles", 5, new int[]{1, 0, 0, 0}, 15,
            SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F,
            () -> Ingredient.of(Items.COPPER_INGOT)),

    // Tier 2: Advanced - iron equivalent
    ADVANCED("advanced_goggles", 15, new int[]{2, 0, 0, 0}, 12,
            SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F,
            () -> Ingredient.of(Items.GOLD_INGOT)),

    // Tier 3: Master - diamond equivalent
    MASTER("master_goggles", 33, new int[]{3, 0, 0, 0}, 10,
            SoundEvents.ARMOR_EQUIP_DIAMOND, 1.0F, 0.0F,
            () -> Ingredient.of(Items.DIAMOND));

    private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11}; // Vanilla values
    private final String name;
    private final int durabilityMultiplier;
    private final int[] slotProtections;
    private final int enchantmentValue;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    PowerGogglesMaterial(String name, int durabilityMultiplier, int[] slotProtections,
                         int enchantmentValue, SoundEvent sound, float toughness,
                         float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.slotProtections = slotProtections;
        this.enchantmentValue = enchantmentValue;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return HEALTH_PER_SLOT[type.getSlot().getIndex()] * this.durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.slotProtections[type.getSlot().getIndex()];
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return BotzPowerScaling.MOD_ID + ":" + this.name;
    }

    @Override
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
