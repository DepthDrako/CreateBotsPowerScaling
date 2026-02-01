package com.botzpowerscaling.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for the Power Scaling mod.
 * Allows customization of attribute-to-stat mappings and weights.
 */
public class PowerScalingConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new CommonConfig(commonBuilder);
        COMMON_SPEC = commonBuilder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    public static class CommonConfig {

        // Stat weights for power calculation
        public final ForgeConfigSpec.DoubleValue healthWeight;
        public final ForgeConfigSpec.DoubleValue attackWeight;
        public final ForgeConfigSpec.DoubleValue defenseWeight;
        public final ForgeConfigSpec.DoubleValue armorWeight;
        public final ForgeConfigSpec.DoubleValue magicWeight;
        public final ForgeConfigSpec.DoubleValue basePowerOffset;

        // Attribute mappings - which attributes contribute to which stats
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> healthAttributes;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> attackAttributes;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> defenseAttributes;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> armorAttributes;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> magicAttributes;

        // Auto-Leveling integration
        public final ForgeConfigSpec.BooleanValue autoLevelingIntegration;
        public final ForgeConfigSpec.BooleanValue showMobLevel;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Power Scaling Configuration")
                   .push("general");

            // Stat weights
            builder.comment("Stat weights for power level calculation",
                           "Power = (Health * healthWeight) + (Attack * attackWeight) + (Defense * defenseWeight) + (Armor * armorWeight) + (Magic * magicWeight) + basePowerOffset");

            healthWeight = builder
                    .comment("Weight multiplier for health in power calculation")
                    .defineInRange("healthWeight", 0.5, 0.0, 10.0);

            attackWeight = builder
                    .comment("Weight multiplier for attack in power calculation")
                    .defineInRange("attackWeight", 1.5, 0.0, 10.0);

            defenseWeight = builder
                    .comment("Weight multiplier for defense in power calculation")
                    .defineInRange("defenseWeight", 1.0, 0.0, 10.0);

            armorWeight = builder
                    .comment("Weight multiplier for armor in power calculation")
                    .defineInRange("armorWeight", 1.25, 0.0, 10.0);

            magicWeight = builder
                    .comment("Weight multiplier for magic in power calculation")
                    .defineInRange("magicWeight", 1.5, 0.0, 10.0);

            basePowerOffset = builder
                    .comment("Base power offset added to all calculations")
                    .defineInRange("basePowerOffset", 11.5, -100.0, 100.0);

            builder.pop();

            // Attribute mappings
            builder.comment("Attribute to Stat Mappings",
                           "Define which Minecraft/mod attributes contribute to each stat",
                           "Format: \"modid:attribute_name\"")
                   .push("attributeMappings");

            healthAttributes = builder
                    .comment("Attributes that contribute to the Health stat (in addition to base max_health)")
                    .defineListAllowEmpty(
                            Arrays.asList("healthAttributes"),
                            () -> Arrays.asList(
                                    // Base max_health is always included, these are additional
                            ),
                            obj -> obj instanceof String
                    );

            attackAttributes = builder
                    .comment("Attributes that contribute to the Attack stat (in addition to base attack_damage)",
                            "Auto-Leveling projectile and explosion damage bonuses are included by default")
                    .defineListAllowEmpty(
                            Arrays.asList("attackAttributes"),
                            () -> Arrays.asList(
                                    "autoleveling:monster.projectile_damage_bonus",
                                    "autoleveling:monster.explosion_damage_bonus"
                            ),
                            obj -> obj instanceof String
                    );

            defenseAttributes = builder
                    .comment("Attributes that contribute to the Defense stat (in addition to armor_toughness)",
                            "Resistance potion effect is also factored into defense")
                    .defineListAllowEmpty(
                            Arrays.asList("defenseAttributes"),
                            () -> Arrays.asList(
                                    // Resistance potion effect is handled separately in code
                            ),
                            obj -> obj instanceof String
                    );

            armorAttributes = builder
                    .comment("Attributes that contribute to the Armor stat (in addition to base armor)")
                    .defineListAllowEmpty(
                            Arrays.asList("armorAttributes"),
                            () -> Arrays.asList(
                                    // Base armor and armor_toughness are always included
                            ),
                            obj -> obj instanceof String
                    );

            magicAttributes = builder
                    .comment("Attributes that contribute to the Magic stat",
                            "Iron's Spellbooks attributes are included by default",
                            "Add other magic mod attributes as needed (e.g., \"ars_nouveau:max_mana\")")
                    .defineListAllowEmpty(
                            Arrays.asList("magicAttributes"),
                            () -> Arrays.asList(
                                    // Iron's Spellbooks attributes
                                    "irons_spellbooks:spell_power",
                                    "irons_spellbooks:spell_resist",
                                    "irons_spellbooks:cooldown_reduction",
                                    "irons_spellbooks:cast_time_reduction",
                                    "irons_spellbooks:max_mana"
                            ),
                            obj -> obj instanceof String
                    );

            builder.pop();

            // Auto-Leveling integration
            builder.comment("Auto-Leveling Mod Integration")
                   .push("autoLeveling");

            autoLevelingIntegration = builder
                    .comment("Enable integration with the Auto-Leveling mod",
                            "When enabled, the scanner will detect mob levels and adjusted attributes")
                    .define("enabled", true);

            showMobLevel = builder
                    .comment("Show the mob's Auto-Leveling level in the UI")
                    .define("showMobLevel", true);

            builder.pop();
        }
    }
}
