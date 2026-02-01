package com.botzpowerscaling.item;

import com.botzpowerscaling.BotzPowerScaling;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for all mod items.
 * Contains the three tiers of Power Goggles.
 */
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BotzPowerScaling.MOD_ID);

    // Tier 1: Basic Power Goggles - shows Rune Level only
    public static final RegistryObject<Item> BASIC_POWER_GOGGLES = ITEMS.register("basic_power_goggles",
            () -> new PowerGogglesItem(
                    PowerGogglesMaterial.BASIC,
                    GogglesTier.BASIC,
                    new Item.Properties().stacksTo(1)
            ));

    // Tier 2: Advanced Power Goggles - shows Rune Level + Stats
    public static final RegistryObject<Item> ADVANCED_POWER_GOGGLES = ITEMS.register("advanced_power_goggles",
            () -> new PowerGogglesItem(
                    PowerGogglesMaterial.ADVANCED,
                    GogglesTier.ADVANCED,
                    new Item.Properties().stacksTo(1)
            ));

    // Tier 3: Master Power Goggles - shows everything
    public static final RegistryObject<Item> MASTER_POWER_GOGGLES = ITEMS.register("master_power_goggles",
            () -> new PowerGogglesItem(
                    PowerGogglesMaterial.MASTER,
                    GogglesTier.MASTER,
                    new Item.Properties().stacksTo(1)
            ));

    /**
     * Register all items to the mod event bus.
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
