package com.botzpowerscaling.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet sent from client to server to share scanned entity info with all players.
 * The server broadcasts the message to everyone.
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
    private final String mainHand;
    private final String offHand;
    private final String head;
    private final String chest;
    private final String legs;
    private final String feet;

    public ShareScanPacket(String entityName, int powerLevel, int hp, int maxHp,
                           int attack, int defense, int armor, int magic, int mobLevel,
                           String mainHand, String offHand, String head,
                           String chest, String legs, String feet) {
        this.entityName = entityName;
        this.powerLevel = powerLevel;
        this.hp = hp;
        this.maxHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.armor = armor;
        this.magic = magic;
        this.mobLevel = mobLevel;
        this.mainHand = mainHand;
        this.offHand = offHand;
        this.head = head;
        this.chest = chest;
        this.legs = legs;
        this.feet = feet;
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
        buf.writeUtf(packet.mainHand);
        buf.writeUtf(packet.offHand);
        buf.writeUtf(packet.head);
        buf.writeUtf(packet.chest);
        buf.writeUtf(packet.legs);
        buf.writeUtf(packet.feet);
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
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf()
        );
    }

    public static void handle(ShareScanPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.getServer() == null) return;

            // Build the shared message
            MutableComponent header = Component.literal("[")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("Scan")
                            .withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("] ")
                            .withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(sender.getName().getString())
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" scanned ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(packet.entityName)
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));

            // Add mob level if present
            if (packet.mobLevel > 0) {
                header.append(Component.literal(" Lv." + packet.mobLevel)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            // Stats line
            MutableComponent stats = Component.literal("  ")
                    .append(Component.literal("PWR ")
                            .withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(String.valueOf(packet.powerLevel))
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" | HP ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(packet.hp + "/" + packet.maxHp)
                            .withStyle(ChatFormatting.RED))
                    .append(Component.literal(" | ATK ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(packet.attack))
                            .withStyle(ChatFormatting.GOLD));

            if (packet.defense > 0) {
                stats.append(Component.literal(" | DEF ")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(packet.defense))
                                .withStyle(ChatFormatting.AQUA));
            }

            if (packet.armor > 0) {
                stats.append(Component.literal(" | ARM ")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(packet.armor))
                                .withStyle(ChatFormatting.GREEN));
            }

            if (packet.magic > 0) {
                stats.append(Component.literal(" | MAG ")
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.valueOf(packet.magic))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            // Equipment line (if any equipment)
            MutableComponent equipment = null;
            boolean hasEquipment = !packet.mainHand.isEmpty() || !packet.head.isEmpty() ||
                    !packet.chest.isEmpty() || !packet.legs.isEmpty() || !packet.feet.isEmpty();

            if (hasEquipment) {
                equipment = Component.literal("  ").withStyle(ChatFormatting.GRAY);
                boolean first = true;

                if (!packet.mainHand.isEmpty()) {
                    equipment.append(Component.literal("Weapon: ")
                                    .withStyle(ChatFormatting.DARK_GRAY))
                            .append(Component.literal(packet.mainHand)
                                    .withStyle(ChatFormatting.RED));
                    first = false;
                }

                int armorCount = 0;
                if (!packet.head.isEmpty()) armorCount++;
                if (!packet.chest.isEmpty()) armorCount++;
                if (!packet.legs.isEmpty()) armorCount++;
                if (!packet.feet.isEmpty()) armorCount++;

                if (armorCount > 0) {
                    if (!first) equipment.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
                    equipment.append(Component.literal("Armor: ")
                                    .withStyle(ChatFormatting.DARK_GRAY))
                            .append(Component.literal(armorCount + "/4 pcs")
                                    .withStyle(ChatFormatting.AQUA));
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
