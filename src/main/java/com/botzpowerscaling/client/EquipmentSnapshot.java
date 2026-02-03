package com.botzpowerscaling.client;

import com.botzpowerscaling.network.NetworkHandler;
import com.botzpowerscaling.network.ShareScanPacket;
import com.botzpowerscaling.scanner.EntityStats;
import com.botzpowerscaling.scanner.MobVariantHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Map;

/**
 * Handles taking equipment snapshots and outputting them to chat.
 * - Double-tap V: Personal snapshot (saves for later sharing)
 * - /scanshare command: Share last saved snapshot with all players
 */
public class EquipmentSnapshot {

    // Last saved snapshot data for sharing
    private static ShareScanPacket lastSnapshot = null;
    private static long lastSnapshotTime = 0;
    private static final long SNAPSHOT_EXPIRY_MS = 60000; // 1 minute expiry

    /**
     * Takes a snapshot of the target entity's equipment and sends it to local chat only.
     * Also saves the snapshot for later sharing via /scanshare.
     *
     * @param entity The entity to snapshot
     * @param stats The entity's stats (for saving)
     */
    public static void takeSnapshot(LivingEntity entity, EntityStats stats) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity == null) {
            return;
        }

        String entityName = MobVariantHelper.getDetailedName(entity);

        // Save snapshot for later sharing
        saveSnapshot(entity, stats, entityName);

        // Header with entity name
        MutableComponent header = Component.literal("═══ ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(entityName)
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(" ═══")
                        .withStyle(ChatFormatting.GOLD));
        mc.player.sendSystemMessage(header);

        // Main hand weapon
        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        sendEquipmentLine(mc, "Main Hand", mainHand, ChatFormatting.RED);

        // Off hand
        ItemStack offHand = entity.getItemBySlot(EquipmentSlot.OFFHAND);
        sendEquipmentLine(mc, "Off Hand", offHand, ChatFormatting.GOLD);

        // Armor (head to feet)
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        sendEquipmentLine(mc, "Head", head, ChatFormatting.AQUA);

        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        sendEquipmentLine(mc, "Chest", chest, ChatFormatting.AQUA);

        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        sendEquipmentLine(mc, "Legs", legs, ChatFormatting.AQUA);

        ItemStack feet = entity.getItemBySlot(EquipmentSlot.FEET);
        sendEquipmentLine(mc, "Feet", feet, ChatFormatting.AQUA);

        // Footer with hint
        MutableComponent footer = Component.literal("═══════════════════")
                .withStyle(ChatFormatting.GOLD);
        mc.player.sendSystemMessage(footer);

        MutableComponent hint = Component.literal("  Use ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("/scanshare")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" to share with others")
                        .withStyle(ChatFormatting.DARK_GRAY));
        mc.player.sendSystemMessage(hint);
    }

    /**
     * Saves the snapshot data for later sharing.
     */
    private static void saveSnapshot(LivingEntity entity, EntityStats stats, String entityName) {
        if (entity == null || stats == null) {
            return;
        }

        // Get equipment names
        String mainHand = getItemName(entity.getItemBySlot(EquipmentSlot.MAINHAND));
        String offHand = getItemName(entity.getItemBySlot(EquipmentSlot.OFFHAND));
        String head = getItemName(entity.getItemBySlot(EquipmentSlot.HEAD));
        String chest = getItemName(entity.getItemBySlot(EquipmentSlot.CHEST));
        String legs = getItemName(entity.getItemBySlot(EquipmentSlot.LEGS));
        String feet = getItemName(entity.getItemBySlot(EquipmentSlot.FEET));

        // Save the packet for later
        lastSnapshot = new ShareScanPacket(
                entityName,
                stats.getDisplayPowerLevel(),
                stats.getDisplayCurrentHealth(),
                stats.getDisplayMaxHealth(),
                stats.getDisplayAttack(),
                stats.getDisplayDefense(),
                stats.getDisplayArmor(),
                stats.getDisplayMagic(),
                stats.getMobLevel(),
                mainHand,
                offHand,
                head,
                chest,
                legs,
                feet
        );
        lastSnapshotTime = System.currentTimeMillis();
    }

    /**
     * Checks if there's a valid saved snapshot that hasn't expired.
     */
    public static boolean hasValidSnapshot() {
        if (lastSnapshot == null) {
            return false;
        }
        // Check if expired (1 minute)
        return (System.currentTimeMillis() - lastSnapshotTime) < SNAPSHOT_EXPIRY_MS;
    }

    /**
     * Shares the last saved snapshot with all players.
     *
     * @return true if shared successfully, false if no valid snapshot
     */
    public static boolean shareLastSnapshot() {
        if (!hasValidSnapshot()) {
            return false;
        }

        NetworkHandler.CHANNEL.sendToServer(lastSnapshot);
        return true;
    }

    /**
     * Gets the name of the entity in the last snapshot (for display).
     */
    public static String getLastSnapshotEntityName() {
        if (lastSnapshot == null) {
            return null;
        }
        return lastSnapshot.getEntityName();
    }

    /**
     * Gets the item name or empty string if the slot is empty.
     */
    private static String getItemName(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        return stack.getHoverName().getString();
    }

    /**
     * Sends a single equipment slot line to chat.
     */
    private static void sendEquipmentLine(Minecraft mc, String slotName, ItemStack stack, ChatFormatting color) {
        if (mc.player == null) return;

        MutableComponent line = Component.literal(" " + slotName + ": ")
                .withStyle(ChatFormatting.GRAY);

        if (stack.isEmpty()) {
            line.append(Component.literal("Empty")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            // Item name
            line.append(Component.literal(stack.getHoverName().getString())
                    .withStyle(color));

            // Enchantments
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            if (!enchants.isEmpty()) {
                StringBuilder enchantStr = new StringBuilder(" [");
                boolean first = true;
                for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    if (!first) enchantStr.append(", ");
                    first = false;

                    String enchantName = getShortEnchantName(entry.getKey());
                    int level = entry.getValue();
                    if (level > 1) {
                        enchantStr.append(enchantName).append(" ").append(level);
                    } else {
                        enchantStr.append(enchantName);
                    }
                }
                enchantStr.append("]");

                line.append(Component.literal(enchantStr.toString())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }

        mc.player.sendSystemMessage(line);
    }

    /**
     * Gets a shortened enchantment name for display.
     */
    private static String getShortEnchantName(Enchantment enchant) {
        String fullName = enchant.getDescriptionId();
        // Extract the last part after the last dot
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fullName.length() - 1) {
            String shortName = fullName.substring(lastDot + 1);
            // Capitalize first letter
            return shortName.substring(0, 1).toUpperCase() + shortName.substring(1);
        }
        return fullName;
    }
}
