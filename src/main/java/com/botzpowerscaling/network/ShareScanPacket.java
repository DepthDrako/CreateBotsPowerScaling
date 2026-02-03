package com.botzpowerscaling.network;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to share scanned entity info with all players.
 * The server broadcasts the message to everyone.
 * Items are sent as NBT data so they can be displayed with hoverable tooltips.
 */
public class ShareScanPacket {

    private final String entityName;
    private final int powerLevel;
    private final int hp;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int armor;
    private final int magic;
    private final int mobLevel;
    private final CompoundTag mainHandTag;
    private final CompoundTag offHandTag;
    private final CompoundTag headTag;
    private final CompoundTag chestTag;
    private final CompoundTag legsTag;
    private final CompoundTag feetTag;

    public ShareScanPacket(String entityName, int powerLevel, int hp, int maxHp,
                           int attack, int defense, int armor, int magic, int mobLevel,
                           CompoundTag mainHandTag, CompoundTag offHandTag, CompoundTag headTag,
                           CompoundTag chestTag, CompoundTag legsTag, CompoundTag feetTag) {
        this.entityName = entityName;
        this.powerLevel = powerLevel;
        this.hp = hp;
        this.maxHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.armor = armor;
        this.magic = magic;
        this.mobLevel = mobLevel;
        this.mainHandTag = mainHandTag;
        this.offHandTag = offHandTag;
        this.headTag = headTag;
        this.chestTag = chestTag;
        this.legsTag = legsTag;
        this.feetTag = feetTag;
    }

    public String getEntityName() {
        return entityName;
    }

    public static void encode(ShareScanPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.entityName);
        buf.writeInt(packet.powerLevel);
        buf.writeInt(packet.hp);
        buf.writeInt(packet.maxHp);
        buf.writeInt(packet.attack);
        buf.writeInt(packet.defense);
        buf.writeInt(packet.armor);
        buf.writeInt(packet.magic);
        buf.writeInt(packet.mobLevel);
        buf.writeNbt(packet.mainHandTag);
        buf.writeNbt(packet.offHandTag);
        buf.writeNbt(packet.headTag);
        buf.writeNbt(packet.chestTag);
        buf.writeNbt(packet.legsTag);
        buf.writeNbt(packet.feetTag);
    }

    public static ShareScanPacket decode(FriendlyByteBuf buf) {
        return new ShareScanPacket(
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readNbt(),
                buf.readNbt(),
                buf.readNbt(),
                buf.readNbt(),
                buf.readNbt(),
                buf.readNbt()
        );
    }

    /**
     * Deserializes an ItemStack from NBT.
     * Returns ItemStack.EMPTY if the tag is empty.
     */
    private static ItemStack deserializeItem(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(tag);
    }

    /**
     * Creates a hoverable item component for chat display.
     */
    private static MutableComponent createHoverableItem(ItemStack stack, ChatFormatting color) {
        if (stack.isEmpty()) {
            return null;
        }

        MutableComponent itemComponent = Component.literal("[")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(stack.getHoverName().copy().withStyle(color))
                .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY));

        // Add hover event to show full item tooltip
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                new HoverEvent.ItemStackInfo(stack));
        return itemComponent.withStyle(Style.EMPTY.withHoverEvent(hoverEvent));
    }

    public static void handle(ShareScanPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.getServer() == null) return;

            // Deserialize items
            ItemStack mainHand = deserializeItem(packet.mainHandTag);
            ItemStack offHand = deserializeItem(packet.offHandTag);
            ItemStack head = deserializeItem(packet.headTag);
            ItemStack chest = deserializeItem(packet.chestTag);
            ItemStack legs = deserializeItem(packet.legsTag);
            ItemStack feet = deserializeItem(packet.feetTag);

            // === Option 3: Emoji/Symbol Enhanced Style ===

            // Line 1: ⚔ Scan: EntityName Lv.X
            MutableComponent header = Component.literal("⚔ ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Scan: ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(packet.entityName)
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

            if (packet.mobLevel > 0) {
                header.append(Component.literal(" Lv." + packet.mobLevel)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            // Line 2: ✦ Power: X  ❤ HP: X/X  ⚔ ATK: X  🛡 DEF: X
            MutableComponent stats = Component.literal("   ✦ ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Power: ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(packet.powerLevel))
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                    .append(Component.literal("  ❤ ")
                            .withStyle(ChatFormatting.RED))
                    .append(Component.literal("HP: ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(packet.hp + "/" + packet.maxHp)
                            .withStyle(ChatFormatting.RED))
                    .append(Component.literal("  ⚔ ")
                            .withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("ATK: ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(packet.attack))
                            .withStyle(ChatFormatting.GOLD));

            if (packet.defense > 0) {
                stats.append(Component.literal("  🛡 ")
                                .withStyle(ChatFormatting.AQUA))
                        .append(Component.literal("DEF: ")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(packet.defense))
                                .withStyle(ChatFormatting.AQUA));
            }

            if (packet.armor > 0) {
                stats.append(Component.literal("  ARM: ")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(packet.armor))
                                .withStyle(ChatFormatting.GREEN));
            }

            if (packet.magic > 0) {
                stats.append(Component.literal("  ✧ ")
                                .withStyle(ChatFormatting.LIGHT_PURPLE))
                        .append(Component.literal("MAG: ")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(packet.magic))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            // Line 3: 📦 [Item1] [Item2] [Item3]...
            MutableComponent equipment = null;
            boolean hasEquipment = !mainHand.isEmpty() || !offHand.isEmpty() ||
                    !head.isEmpty() || !chest.isEmpty() || !legs.isEmpty() || !feet.isEmpty();

            if (hasEquipment) {
                equipment = Component.literal("   📦 ").withStyle(ChatFormatting.AQUA);

                if (!mainHand.isEmpty()) {
                    equipment.append(createHoverableItem(mainHand, ChatFormatting.RED));
                    equipment.append(Component.literal(" "));
                }

                if (!offHand.isEmpty()) {
                    equipment.append(createHoverableItem(offHand, ChatFormatting.GOLD));
                    equipment.append(Component.literal(" "));
                }

                if (!head.isEmpty()) {
                    equipment.append(createHoverableItem(head, ChatFormatting.AQUA));
                    equipment.append(Component.literal(" "));
                }

                if (!chest.isEmpty()) {
                    equipment.append(createHoverableItem(chest, ChatFormatting.AQUA));
                    equipment.append(Component.literal(" "));
                }

                if (!legs.isEmpty()) {
                    equipment.append(createHoverableItem(legs, ChatFormatting.AQUA));
                    equipment.append(Component.literal(" "));
                }

                if (!feet.isEmpty()) {
                    equipment.append(createHoverableItem(feet, ChatFormatting.AQUA));
                }
            }

            // Broadcast to all players
            for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
                player.sendSystemMessage(header);
                player.sendSystemMessage(stats);
                if (equipment != null) {
                    player.sendSystemMessage(equipment);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
