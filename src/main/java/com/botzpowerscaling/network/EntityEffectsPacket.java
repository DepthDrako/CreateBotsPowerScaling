package com.botzpowerscaling.network;

import com.botzpowerscaling.scanner.EffectDataCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet sent from server to client containing entity effect data.
 * This allows the client to know about potion effects on other entities.
 * Now includes all active effects for icon display.
 */
public class EntityEffectsPacket {

    private final int entityId;
    private final int strengthLevel;    // 0 = no effect, 1+ = effect level
    private final int weaknessLevel;
    private final int resistanceLevel;
    private final List<EffectEntry> allEffects;  // All active effects for icon display

    public EntityEffectsPacket(int entityId, int strengthLevel, int weaknessLevel, int resistanceLevel,
                                List<EffectEntry> allEffects) {
        this.entityId = entityId;
        this.strengthLevel = strengthLevel;
        this.weaknessLevel = weaknessLevel;
        this.resistanceLevel = resistanceLevel;
        this.allEffects = allEffects;
    }

    public static void encode(EntityEffectsPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeInt(packet.strengthLevel);
        buf.writeInt(packet.weaknessLevel);
        buf.writeInt(packet.resistanceLevel);

        // Write all effects
        buf.writeInt(packet.allEffects.size());
        for (EffectEntry effect : packet.allEffects) {
            buf.writeResourceLocation(effect.effectId);
            buf.writeInt(effect.amplifier);
        }
    }

    public static EntityEffectsPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        int strengthLevel = buf.readInt();
        int weaknessLevel = buf.readInt();
        int resistanceLevel = buf.readInt();

        int effectCount = buf.readInt();
        List<EffectEntry> effects = new ArrayList<>(effectCount);
        for (int i = 0; i < effectCount; i++) {
            ResourceLocation effectId = buf.readResourceLocation();
            int amplifier = buf.readInt();
            effects.add(new EffectEntry(effectId, amplifier));
        }

        return new EntityEffectsPacket(entityId, strengthLevel, weaknessLevel, resistanceLevel, effects);
    }

    public static void handle(EntityEffectsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Store the effect data in our client-side cache
            EffectDataCache.setEffectData(
                    packet.entityId,
                    packet.strengthLevel,
                    packet.weaknessLevel,
                    packet.resistanceLevel,
                    packet.allEffects
            );
        });
        context.setPacketHandled(true);
    }

    /**
     * Simple record to hold effect ID and amplifier for network transmission.
     */
    public record EffectEntry(ResourceLocation effectId, int amplifier) {}
}
