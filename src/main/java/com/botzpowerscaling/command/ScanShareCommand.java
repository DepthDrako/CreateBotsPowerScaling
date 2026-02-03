package com.botzpowerscaling.command;

import com.botzpowerscaling.client.EquipmentSnapshot;
import com.botzpowerscaling.item.GogglesTier;
import com.botzpowerscaling.item.PowerGogglesItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side command to share the last scanned entity info with all players.
 * Usage: /scanshare
 *
 * Workflow:
 * 1. Wear Master Power Goggles
 * 2. Hold V and look at an entity
 * 3. Double-tap V to take a snapshot (saves locally and shows in your chat)
 * 4. Type /scanshare to share that snapshot with everyone
 *
 * The snapshot expires after 1 minute.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScanShareCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("scanshare")
                        .executes(ScanShareCommand::execute)
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            return 0;
        }

        // Check if wearing Master goggles
        GogglesTier tier = PowerGogglesItem.getWornTier(mc.player);
        if (tier != GogglesTier.MASTER) {
            mc.player.sendSystemMessage(
                    Component.literal("You need to wear Master Power Goggles to share scans!")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        // Check if we have a saved snapshot
        if (!EquipmentSnapshot.hasValidSnapshot()) {
            mc.player.sendSystemMessage(
                    Component.literal("No recent scan to share! Double-tap V while scanning an entity first.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        // Share the saved snapshot
        String entityName = EquipmentSnapshot.getLastSnapshotEntityName();
        if (EquipmentSnapshot.shareLastSnapshot()) {
            mc.player.sendSystemMessage(
                    Component.literal("Shared scan of ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(entityName != null ? entityName : "entity")
                                    .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal(" with all players!")
                                    .withStyle(ChatFormatting.GREEN))
            );
            return 1;
        } else {
            mc.player.sendSystemMessage(
                    Component.literal("Failed to share scan.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }
    }
}
