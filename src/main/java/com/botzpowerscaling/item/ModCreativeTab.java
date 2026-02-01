package com.botzpowerscaling.item;

import com.botzpowerscaling.BotzPowerScaling;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Creative mode tab for Power Scaling mod items.
 */
public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BotzPowerScaling.MOD_ID);

    public static final RegistryObject<CreativeModeTab> POWER_SCALING_TAB = CREATIVE_TABS.register("power_scaling_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MASTER_POWER_GOGGLES.get()))
                    .title(Component.translatable("itemGroup." + BotzPowerScaling.MOD_ID + ".power_scaling_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BASIC_POWER_GOGGLES.get());
                        output.accept(ModItems.ADVANCED_POWER_GOGGLES.get());
                        output.accept(ModItems.MASTER_POWER_GOGGLES.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
