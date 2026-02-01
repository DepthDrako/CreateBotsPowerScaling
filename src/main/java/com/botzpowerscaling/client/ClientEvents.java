package com.botzpowerscaling.client;

import com.botzpowerscaling.BotzPowerScaling;
import com.botzpowerscaling.scanner.EntityScanner;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side event handlers for the power scanner.
 *
 * Handles:
 * - Registering the GUI overlay with Forge
 * - Registering keybindings
 * - Client tick updates for the entity scanner
 */
public class ClientEvents {

    /**
     * MOD event bus subscriber - handles mod loading events.
     * Used for registering overlays and keybindings during mod setup.
     */
    @Mod.EventBusSubscriber(modid = BotzPowerScaling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {

        /**
         * Registers our custom GUI overlay with Forge's overlay system.
         * The overlay renders after the crosshair, so it appears in front.
         */
        @SubscribeEvent
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            // Register our overlay to render after the crosshair
            // This ensures it's visible and in a good screen position
            event.registerAbove(
                    VanillaGuiOverlay.CROSSHAIR.id(),
                    "power_scanner",
                    PowerScannerOverlay.OVERLAY
            );

            BotzPowerScaling.LOGGER.info("Registered Power Scanner overlay");
        }

        /**
         * Registers keybindings for the power scanner.
         */
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            KeyBindings.register(event);
        }
    }

    /**
     * FORGE event bus subscriber - handles runtime game events.
     * Used for tick updates and world events.
     */
    @Mod.EventBusSubscriber(modid = BotzPowerScaling.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {

        /**
         * Client tick handler - updates the entity scanner each tick.
         * Only runs during the END phase to avoid double-processing.
         */
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            // Update the scanner's internal state
            EntityScanner.getInstance().tick();
        }
    }
}
