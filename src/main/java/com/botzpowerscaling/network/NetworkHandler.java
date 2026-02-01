package com.botzpowerscaling.network;

import com.botzpowerscaling.BotzPowerScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Handles network communication between server and client.
 * Used to sync entity effect data that isn't normally available on clients.
 */
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BotzPowerScaling.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // Register packets
        CHANNEL.registerMessage(
                packetId++,
                EntityEffectsPacket.class,
                EntityEffectsPacket::encode,
                EntityEffectsPacket::decode,
                EntityEffectsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        CHANNEL.registerMessage(
                packetId++,
                RequestEntityEffectsPacket.class,
                RequestEntityEffectsPacket::encode,
                RequestEntityEffectsPacket::decode,
                RequestEntityEffectsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        BotzPowerScaling.LOGGER.info("Registered network packets");
    }
}
