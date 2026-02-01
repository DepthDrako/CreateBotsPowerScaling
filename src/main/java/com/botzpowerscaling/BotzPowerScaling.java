package com.botzpowerscaling;

import com.botzpowerscaling.config.PowerScalingConfig;
import com.botzpowerscaling.item.ModCreativeTab;
import com.botzpowerscaling.item.ModItems;
import com.botzpowerscaling.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * BotzPowerScaling - A Create mod addon that adds a power scanner feature.
 *
 * When wearing Power Goggles (any tier) or Create goggles and looking at any
 * living entity (players, mobs), displays an overlay showing combat stats.
 *
 * GOGGLES TIERS:
 * - Tier 1 (Basic): Rune Level only
 * - Tier 2 (Advanced): Rune Level + Stats (HP, ATK, DEF, ARM, MAG)
 * - Tier 3 (Master): Everything including potion effects and mob level
 * - Create Goggles: Treated as Master tier for backwards compatibility
 *
 * Calibration: Full Netherite armor (no enchants/effects) = Power Level 60
 *
 * @see com.botzpowerscaling.scanner.StatsCalculator for stat calculation details
 * @see com.botzpowerscaling.config.PowerScalingConstants for tuning constants
 */
@Mod(BotzPowerScaling.MOD_ID)
public class BotzPowerScaling {

    public static final String MOD_ID = "botzpowerscaling";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BotzPowerScaling() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        PowerScalingConfig.register();

        // Register items
        ModItems.register(modEventBus);

        // Register creative tab
        ModCreativeTab.register(modEventBus);

        // Register common setup listener
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("BotzPowerScaling initialized");
    }

    /**
     * Common setup - runs on both client and server.
     * Used for things that need to happen on both sides.
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Register network packets
            NetworkHandler.register();
        });
        LOGGER.info("BotzPowerScaling common setup complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.debug("BotzPowerScaling server starting");
    }

    /**
     * Client-side mod events subscriber.
     * Handles client-specific setup.
     */
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("BotzPowerScaling client setup - Power Scanner ready!");
            LOGGER.info("Wear Power Goggles or Create goggles and look at entities to scan their power level.");
        }
    }
}
