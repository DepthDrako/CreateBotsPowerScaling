package com.botzpowerscaling.client;

import com.botzpowerscaling.network.NetworkHandler;
import com.botzpowerscaling.network.ShareScanPacket;
import com.botzpowerscaling.scanner.EntityStats;
import com.botzpowerscaling.scanner.MobVariantHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

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
     * Uses Option 5: Detailed Card Style format.
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

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ (top border)
        mc.player.sendSystemMessage(Component.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .withStyle(ChatFormatting.GOLD));

        // SCAN: Entity Name (Lv. X)
        MutableComponent titleLine = Component.literal("  SCAN: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(entityName)
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        if (stats.hasMobLevel()) {
            titleLine.append(Component.literal(" (Lv. " + stats.getMobLevel() + ")")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        mc.player.sendSystemMessage(titleLine);

        // Scanned by: PlayerName
        mc.player.sendSystemMessage(Component.literal("  Scanned by: ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(mc.player.getName().getString())
                        .withStyle(ChatFormatting.WHITE)));

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ (separator)
        mc.player.sendSystemMessage(Component.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .withStyle(ChatFormatting.DARK_GRAY));

        // Power Level: X
        mc.player.sendSystemMessage(Component.literal("  Power Level: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(stats.getDisplayPowerLevel()))
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));

        // Stats line: HP: X/X | ATK: X | DEF: X | ARM: X
        MutableComponent statsLine = Component.literal("  ")
                .append(Component.literal("HP: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(stats.getDisplayCurrentHealth() + "/" + stats.getDisplayMaxHealth())
                        .withStyle(ChatFormatting.RED))
                .append(Component.literal(" | ATK: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(stats.getDisplayAttack()))
                        .withStyle(ChatFormatting.GOLD));

        if (stats.hasDefense()) {
            statsLine.append(Component.literal(" | DEF: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(stats.getDisplayDefense()))
                            .withStyle(ChatFormatting.AQUA));
        }
        if (stats.hasArmor()) {
            statsLine.append(Component.literal(" | ARM: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(stats.getDisplayArmor()))
                            .withStyle(ChatFormatting.GREEN));
        }
        if (stats.hasMagic()) {
            statsLine.append(Component.literal(" | MAG: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(stats.getDisplayMagic()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        mc.player.sendSystemMessage(statsLine);

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ (separator)
        mc.player.sendSystemMessage(Component.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .withStyle(ChatFormatting.DARK_GRAY));

        // Equipment: header
        mc.player.sendSystemMessage(Component.literal("  Equipment:")
                .withStyle(ChatFormatting.GRAY));

        // Equipment slots with arrows
        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offHand = entity.getItemBySlot(EquipmentSlot.OFFHAND);
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = entity.getItemBySlot(EquipmentSlot.FEET);

        sendEquipmentArrowLine(mc, mainHand, ChatFormatting.RED);
        sendEquipmentArrowLine(mc, offHand, ChatFormatting.GOLD);
        sendEquipmentArrowLine(mc, head, ChatFormatting.AQUA);
        sendEquipmentArrowLine(mc, chest, ChatFormatting.AQUA);
        sendEquipmentArrowLine(mc, legs, ChatFormatting.AQUA);
        sendEquipmentArrowLine(mc, feet, ChatFormatting.AQUA);

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ (bottom border)
        mc.player.sendSystemMessage(Component.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                .withStyle(ChatFormatting.GOLD));

        // Hint for sharing
        mc.player.sendSystemMessage(Component.literal("  Use ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal("/scanshare")
                        .withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" to share with others")
                        .withStyle(ChatFormatting.DARK_GRAY)));
    }

    /**
     * Sends an equipment line with arrow prefix (only for non-empty items).
     */
    private static void sendEquipmentArrowLine(Minecraft mc, ItemStack stack, ChatFormatting color) {
        if (mc.player == null || stack.isEmpty()) return;

        MutableComponent line = Component.literal("    → ")
                .withStyle(ChatFormatting.DARK_GRAY);

        // Create hoverable item component with full tooltip
        MutableComponent itemComponent = Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(stack.getHoverName().copy().withStyle(color))
                .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));

        // Add hover event to show full item tooltip
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                new HoverEvent.ItemStackInfo(stack));
        itemComponent = itemComponent.withStyle(Style.EMPTY.withHoverEvent(hoverEvent));

        line.append(itemComponent);
        mc.player.sendSystemMessage(line);
    }

    /**
     * Saves the snapshot data for later sharing.
     */
    private static void saveSnapshot(LivingEntity entity, EntityStats stats, String entityName) {
        if (entity == null || stats == null) {
            return;
        }

        // Get equipment NBT data (for hoverable items in shared chat)
        CompoundTag mainHandTag = serializeItem(entity.getItemBySlot(EquipmentSlot.MAINHAND));
        CompoundTag offHandTag = serializeItem(entity.getItemBySlot(EquipmentSlot.OFFHAND));
        CompoundTag headTag = serializeItem(entity.getItemBySlot(EquipmentSlot.HEAD));
        CompoundTag chestTag = serializeItem(entity.getItemBySlot(EquipmentSlot.CHEST));
        CompoundTag legsTag = serializeItem(entity.getItemBySlot(EquipmentSlot.LEGS));
        CompoundTag feetTag = serializeItem(entity.getItemBySlot(EquipmentSlot.FEET));

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
                mainHandTag,
                offHandTag,
                headTag,
                chestTag,
                legsTag,
                feetTag
        );
        lastSnapshotTime = System.currentTimeMillis();
    }

    /**
     * Serializes an ItemStack to NBT for network transmission.
     * Returns empty CompoundTag if the stack is empty.
     */
    private static CompoundTag serializeItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.save(new CompoundTag());
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

}
