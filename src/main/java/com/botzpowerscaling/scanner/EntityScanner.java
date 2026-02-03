package com.botzpowerscaling.scanner;

import com.botzpowerscaling.config.PowerScalingConstants;
import com.botzpowerscaling.item.GogglesTier;
import com.botzpowerscaling.network.NetworkHandler;
import com.botzpowerscaling.network.RequestEntityEffectsPacket;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Handles entity targeting via ray tracing with caching for performance.
 *
 * This class manages:
 * - Ray tracing from the player's view to find targeted entities
 * - Caching of the last targeted entity and its stats
 * - Cache invalidation based on time and target changes
 */
public class EntityScanner {

    // Singleton instance for client-side scanning
    private static final EntityScanner INSTANCE = new EntityScanner();

    // Cached data
    @Nullable
    private LivingEntity cachedTarget;
    @Nullable
    private EntityStats cachedStats;
    private long cacheTimestamp;
    private int cacheTickCounter;

    // Fade animation state
    private int fadeInTicks;
    private boolean wasTargeting;

    // Network request tracking
    private int lastRequestedEntityId = -1;
    private long lastRequestTime = 0;
    private static final long REQUEST_COOLDOWN_MS = 200; // Request effect data every 200ms

    private EntityScanner() {
        this.cachedTarget = null;
        this.cachedStats = null;
        this.cacheTimestamp = 0;
        this.cacheTickCounter = 0;
        this.fadeInTicks = 0;
        this.wasTargeting = false;
    }

    public static EntityScanner getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the current target entity and its stats.
     * Uses caching to avoid expensive recalculations every frame.
     *
     * @param player The player doing the scanning
     * @param tier The goggles tier (determines scan range)
     * @return EntityStats for the targeted entity, or empty stats if none
     */
    public EntityStats getTargetStats(Player player, GogglesTier tier) {
        if (player == null || tier == GogglesTier.NONE) {
            resetCache();
            return EntityStats.empty();
        }

        // Perform ray trace to find target using tier-specific range
        LivingEntity currentTarget = findTargetEntity(player, tier.getScanRange());

        // Check if target changed
        if (currentTarget != cachedTarget) {
            // Target changed - reset fade and cache
            cachedTarget = currentTarget;
            cacheTickCounter = 0;

            if (currentTarget != null) {
                // New target - start fade in
                if (!wasTargeting) {
                    fadeInTicks = 0;
                }
                // Request effect data from server for this entity
                requestEntityEffects(currentTarget);
                cachedStats = StatsCalculator.calculate(currentTarget);
                wasTargeting = true;
            } else {
                // Lost target
                cachedStats = EntityStats.empty();
                wasTargeting = false;
                fadeInTicks = 0;
            }
        } else if (currentTarget != null) {
            // Same target - check if cache needs refresh
            cacheTickCounter++;
            if (cacheTickCounter >= PowerScalingConstants.CACHE_DURATION_TICKS) {
                // Request updated effect data from server
                requestEntityEffects(currentTarget);
                // Refresh stats
                cachedStats = StatsCalculator.calculate(currentTarget);
                cacheTickCounter = 0;
            }

            // Continue fade in
            if (fadeInTicks < PowerScalingConstants.OVERLAY_FADE_TICKS) {
                fadeInTicks++;
            }
        }

        return cachedStats != null ? cachedStats : EntityStats.empty();
    }

    /**
     * Requests effect data from the server for the given entity.
     * Uses a cooldown to prevent spamming the server.
     */
    private void requestEntityEffects(LivingEntity entity) {
        if (entity == null) return;

        int entityId = entity.getId();
        long currentTime = System.currentTimeMillis();

        // Only request if it's a different entity or cooldown has passed
        if (entityId != lastRequestedEntityId || (currentTime - lastRequestTime) >= REQUEST_COOLDOWN_MS) {
            lastRequestedEntityId = entityId;
            lastRequestTime = currentTime;

            // Send request to server
            NetworkHandler.CHANNEL.sendToServer(new RequestEntityEffectsPacket(entityId));
        }
    }

    /**
     * Gets the current fade-in progress (0.0 to 1.0).
     * Used for smooth overlay appearance animation.
     */
    public float getFadeProgress() {
        if (fadeInTicks >= PowerScalingConstants.OVERLAY_FADE_TICKS) {
            return 1.0f;
        }
        return (float) fadeInTicks / PowerScalingConstants.OVERLAY_FADE_TICKS;
    }

    /**
     * @return true if currently targeting a valid entity
     */
    public boolean hasTarget() {
        return cachedTarget != null && cachedStats != null && cachedStats.isValid();
    }

    /**
     * @return The currently targeted entity, or null
     */
    @Nullable
    public LivingEntity getTarget() {
        return cachedTarget;
    }

    /**
     * Performs a ray trace from the camera position to find a living entity.
     * Uses the actual camera position and direction, which works correctly with
     * third-person camera mods (like Shoulder Surfing, etc.).
     *
     * @param player The player to trace from
     * @param range The maximum scan range in blocks (determined by goggles tier)
     * @return The targeted LivingEntity, or null if none found
     */
    @Nullable
    private LivingEntity findTargetEntity(Player player, double range) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }

        // Use the camera position and look direction instead of player position
        // This works correctly with third-person camera mods
        Camera camera = mc.gameRenderer.getMainCamera();

        // Safety check - camera might not be ready
        if (!camera.isInitialized()) {
            // Fallback to player-based targeting
            return findTargetEntityFromPlayer(player, range);
        }

        Vec3 cameraPos = camera.getPosition();

        // Use camera's getLookVector which correctly handles rotation
        Vec3 lookVec = new Vec3(camera.getLookVector());

        Vec3 endPos = cameraPos.add(lookVec.scale(range));

        // Create bounding box for entity search - use camera position as center
        AABB searchBox = new AABB(cameraPos, cameraPos).expandTowards(lookVec.scale(range)).inflate(1.0);

        // Filter: only living entities, not self, not dead
        Predicate<Entity> filter = entity ->
                entity instanceof LivingEntity
                        && entity != player
                        && !entity.isSpectator()
                        && entity.isAlive()
                        && entity.isPickable();

        // Perform ray trace from camera position
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player,
                cameraPos,
                endPos,
                searchBox,
                filter,
                range * range // squared distance
        );

        if (hitResult != null && hitResult.getEntity() instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    /**
     * Fallback method that traces from the player's eye position.
     * Used when camera is not initialized.
     *
     * @param player The player to trace from
     * @param range The scan range
     * @return The targeted LivingEntity, or null if none found
     */
    @Nullable
    private LivingEntity findTargetEntityFromPlayer(Player player, double range) {
        Minecraft mc = Minecraft.getInstance();

        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);

        Predicate<Entity> filter = entity ->
                entity instanceof LivingEntity
                        && entity != player
                        && !entity.isSpectator()
                        && entity.isAlive()
                        && entity.isPickable();

        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                player,
                eyePos,
                endPos,
                searchBox,
                filter,
                range * range
        );

        if (hitResult != null && hitResult.getEntity() instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    /**
     * Resets all cached data.
     * Called when the scanner should fully reset (e.g., world change).
     */
    public void resetCache() {
        cachedTarget = null;
        cachedStats = null;
        cacheTickCounter = 0;
        fadeInTicks = 0;
        wasTargeting = false;
    }

    /**
     * Called each client tick to update internal state.
     */
    public void tick() {
        // Validate cached target still exists and is alive
        if (cachedTarget != null) {
            if (cachedTarget.isRemoved() || !cachedTarget.isAlive()) {
                resetCache();
            }
        }
    }
}
