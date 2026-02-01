package com.botzpowerscaling.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet sent from client to server requesting effect data for an entity.
 * Server responds with EntityEffectsPacket containing the effect levels.
 */
public class RequestEntityEffectsPacket {

    private final int entityId;

    public RequestEntityEffectsPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(RequestEntityEffectsPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
    }

    public static RequestEntityEffectsPacket decode(FriendlyByteBuf buf) {
        return new RequestEntityEffectsPacket(buf.readInt());
    }

    public static void handle(RequestEntityEffectsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Entity entity = player.level().getEntity(packet.entityId);
            if (entity instanceof LivingEntity livingEntity) {
                // Get specific effect levels for stat calculations
                int strengthLevel = 0;
                int weaknessLevel = 0;
                int resistanceLevel = 0;

                var strength = livingEntity.getEffect(MobEffects.DAMAGE_BOOST);
                if (strength != null) {
                    strengthLevel = strength.getAmplifier() + 1;
                }

                var weakness = livingEntity.getEffect(MobEffects.WEAKNESS);
                if (weakness != null) {
                    weaknessLevel = weakness.getAmplifier() + 1;
                }

                var resistance = livingEntity.getEffect(MobEffects.DAMAGE_RESISTANCE);
                if (resistance != null) {
                    resistanceLevel = resistance.getAmplifier() + 1;
                }

                // Collect all active effects for icon display
                Collection<MobEffectInstance> activeEffects = livingEntity.getActiveEffects();
                List<EntityEffectsPacket.EffectEntry> effectEntries = new ArrayList<>();

                for (MobEffectInstance effectInstance : activeEffects) {
                    MobEffect effect = effectInstance.getEffect();
                    ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                    if (effectId != null) {
                        effectEntries.add(new EntityEffectsPacket.EffectEntry(effectId, effectInstance.getAmplifier()));
                    }
                }

                // Send response back to the requesting client
                EntityEffectsPacket response = new EntityEffectsPacket(
                        packet.entityId,
                        strengthLevel,
                        weaknessLevel,
                        resistanceLevel,
                        effectEntries
                );

                NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        response
                );
            }
        });
        context.setPacketHandled(true);
    }
}
