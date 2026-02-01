package com.botzpowerscaling.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base class for all Power Goggles items.
 * Each tier unlocks different scanning capabilities.
 */
public class PowerGogglesItem extends ArmorItem {

    private final GogglesTier tier;

    public PowerGogglesItem(ArmorMaterial material, GogglesTier tier, Properties properties) {
        super(material, Type.HELMET, properties);
        this.tier = tier;
    }

    public GogglesTier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // Add tier-specific tooltip information
        switch (tier) {
            case BASIC:
                tooltip.add(Component.literal("Tier I - Basic Scanner").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("Shows:").withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal(" • Rune Level").withStyle(ChatFormatting.WHITE));
                break;

            case ADVANCED:
                tooltip.add(Component.literal("Tier II - Advanced Scanner").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("Shows:").withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal(" • Rune Level").withStyle(ChatFormatting.WHITE));
                tooltip.add(Component.literal(" • Health Bar").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal(" • Attack / Defense / Armor").withStyle(ChatFormatting.WHITE));
                tooltip.add(Component.literal(" • Magic").withStyle(ChatFormatting.LIGHT_PURPLE));
                break;

            case MASTER:
                tooltip.add(Component.literal("Tier III - Master Scanner").withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("Shows:").withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal(" • Rune Level").withStyle(ChatFormatting.WHITE));
                tooltip.add(Component.literal(" • Health Bar").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal(" • Attack / Defense / Armor").withStyle(ChatFormatting.WHITE));
                tooltip.add(Component.literal(" • Magic").withStyle(ChatFormatting.LIGHT_PURPLE));
                tooltip.add(Component.literal(" • Potion Effects").withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.literal(" • Monster Level").withStyle(ChatFormatting.YELLOW));
                break;

            default:
                break;
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Hold [V] while looking at an entity").withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * Gets the goggles tier from the player's helmet slot.
     * Checks for both custom Power Goggles and Create goggles (treated as MASTER tier).
     *
     * @param player The player to check
     * @return The tier of goggles worn, or NONE if no valid goggles
     */
    public static GogglesTier getWornTier(Player player) {
        if (player == null) {
            return GogglesTier.NONE;
        }

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) {
            return GogglesTier.NONE;
        }

        // Check for our custom Power Goggles
        if (helmet.getItem() instanceof PowerGogglesItem powerGoggles) {
            return powerGoggles.getTier();
        }

        // Check for Create mod goggles - treat as ADVANCED tier (stats only, no effects/mob level)
        if (isCreateGoggles(helmet)) {
            return GogglesTier.ADVANCED;
        }

        return GogglesTier.NONE;
    }

    /**
     * Checks if the item is Create mod's goggles.
     */
    private static boolean isCreateGoggles(ItemStack stack) {
        try {
            // Check if item is an instance of Create's GogglesItem
            return stack.getItem() instanceof com.simibubi.create.content.equipment.goggles.GogglesItem;
        } catch (NoClassDefFoundError | Exception e) {
            // Fallback: check by class name if Create API fails or isn't loaded
            String className = stack.getItem().getClass().getName();
            return className.contains("GogglesItem") && className.contains("create");
        }
    }

    /**
     * Alternative method to check Create goggles directly on player.
     * This is used for backwards compatibility.
     */
    public static boolean isWearingCreateGoggles(Player player) {
        try {
            return com.simibubi.create.content.equipment.goggles.GogglesItem.isWearingGoggles(player);
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }

    /**
     * Check if player is wearing any valid scanning goggles (custom or Create).
     */
    public static boolean isWearingAnyGoggles(Player player) {
        return getWornTier(player) != GogglesTier.NONE;
    }
}
