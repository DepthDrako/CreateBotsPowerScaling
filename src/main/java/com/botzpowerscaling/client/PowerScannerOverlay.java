package com.botzpowerscaling.client;

import com.botzpowerscaling.config.PowerScalingConstants;
import com.botzpowerscaling.integration.AutoLevelingIntegration;
import com.botzpowerscaling.item.GogglesTier;
import com.botzpowerscaling.item.PowerGogglesItem;
import com.botzpowerscaling.network.EntityEffectsPacket;
import com.botzpowerscaling.scanner.EffectDataCache;
import com.botzpowerscaling.scanner.EntityScanner;
import com.botzpowerscaling.scanner.EntityStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Renders an Elden Ring-style power scanner overlay.
 *
 * Features:
 * - Dark, muted aesthetic inspired by FromSoftware UI
 * - Subtle golden accents on dark background
 * - Elegant, understated stat display
 * - Faded parchment-like feel
 * - Auto-Leveling mod integration (shows mob level)
 * - Magic stat support
 * - Tier-based information display
 *
 * GOGGLES TIERS:
 * - Tier 1 (Basic): Rune Level only
 * - Tier 2 (Advanced): Rune Level + Stats (HP, ATK, DEF, ARM, MAG)
 * - Tier 3 (Master): Everything including potion effects and mob level
 * - Create Goggles: Treated as Master tier for backwards compatibility
 *
 * ACTIVATION:
 * - Must be wearing Power Goggles (any tier) or Create goggles
 * - Must hold the scanner key (default: V, configurable in Controls)
 * - Must be looking at a living entity within range
 */
public class PowerScannerOverlay {

    // Elden Ring color palette - dark and muted
    private static final int BG_DARK = 0x0D0B09;           // Near black with warm tint
    private static final int BG_INNER = 0x1A1714;          // Dark brown-gray
    private static final int BORDER_DARK = 0x3D3428;       // Muted dark gold
    private static final int BORDER_ACCENT = 0x8B7355;     // Faded gold accent
    private static final int BORDER_HIGHLIGHT = 0xC9A227;  // Elden Ring gold

    // Stat colors (muted, FromSoft style)
    private static final int RUNE_COLOR = 0xC9A227;        // Elden Ring gold for power
    private static final int HEALTH_COLOR = 0x8B2020;      // Dark crimson
    private static final int ATTACK_COLOR = 0xB85C38;      // Burnt orange
    private static final int DEFENSE_COLOR = 0x4A6B8A;     // Steel blue
    private static final int ARMOR_COLOR = 0x5C7C5C;       // Muted green
    private static final int MAGIC_COLOR = 0x7B68EE;       // Medium slate blue (magic)
    private static final int MOB_LEVEL_COLOR = 0xAA88FF;   // Light purple for mob level

    // Text colors
    private static final int TEXT_LIGHT = 0xD4C4A8;        // Parchment white
    private static final int TEXT_GOLD = 0xC9A227;         // Elden gold
    private static final int TEXT_GRAY = 0x7A7060;         // Muted gray
    private static final int TEXT_NAME = 0xE8D5A3;         // Warm white for names
    private static final int BONUS_POSITIVE = 0x50C878;    // Green for positive bonus
    private static final int BONUS_NEGATIVE = 0xB84A4A;    // Red for negative bonus

    // UI dimensions - portrait style (narrower, taller)
    private static final int PANEL_WIDTH = 130;
    private static final int BASE_PANEL_HEIGHT = 118;
    private static final int BAR_WIDTH = 50;
    private static final int BAR_HEIGHT = 4;
    private static final int EFFECT_ICON_SIZE = 18;  // Size of potion effect icons
    private static final int PANEL_RIGHT_MARGIN = 10; // Distance from right edge of screen
    private static final float BG_OPACITY = 0.60f;   // 60% opacity for background

    /**
     * The IGuiOverlay instance to register with Forge.
     */
    public static final IGuiOverlay OVERLAY = PowerScannerOverlay::renderOverlay;

    /**
     * Main render method called by Forge's GUI overlay system.
     */
    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Check goggles tier - supports both custom Power Goggles and Create goggles
        GogglesTier tier = PowerGogglesItem.getWornTier(mc.player);
        if (tier == GogglesTier.NONE) {
            EntityScanner.getInstance().resetCache();
            return;
        }

        // Check if scanner key is being held
        if (!KeyBindings.isScannerKeyHeld()) {
            EntityScanner.getInstance().resetCache();
            return;
        }

        // Don't render in certain GUI situations
        if (mc.options.hideGui || mc.screen != null) {
            return;
        }

        // Get target stats from scanner
        EntityStats stats = EntityScanner.getInstance().getTargetStats(mc.player);

        if (!stats.isValid()) {
            return;
        }

        // Check for double-tap snapshot (Master tier only)
        if (tier.canShowEquipment() && KeyBindings.consumeDoubleTap()) {
            LivingEntity target = EntityScanner.getInstance().getTarget();
            if (target != null) {
                EquipmentSnapshot.takeSnapshot(target, stats);
            }
        }

        // Get fade progress for animation
        float fade = EntityScanner.getInstance().getFadeProgress();
        if (fade <= 0) {
            return;
        }

        // Render the fantasy-style panel with tier-based features
        renderFantasyPanel(graphics, stats, width, height, fade, mc.font, tier);
    }

    /**
     * Calculates panel height based on what content needs to be displayed and goggles tier.
     * Portrait layout: narrower panel, stats stacked vertically.
     */
    private static int calculatePanelHeight(EntityStats stats, GogglesTier tier) {
        // Tier 1: Just name and power level
        if (tier == GogglesTier.BASIC) {
            return 52; // Compact panel for basic info only
        }

        // Base height for Tier 2+: name + separator + power level + HP/ATK row
        int height = 70;

        // Add space for mob level if present (Tier 3 only)
        if (tier.canShowMobLevel() && stats.hasMobLevel() && AutoLevelingIntegration.shouldShowMobLevel()) {
            height += 12;
        }

        // Add row for DEF/ARM if either is present (Tier 2+)
        if (tier.canShowStats() && (stats.hasDefense() || stats.hasArmor())) {
            height += 12;
        }

        // Add row for MAG if present (Tier 2+)
        if (tier.canShowStats() && stats.hasMagic()) {
            height += 12;
        }

        // Add equipment section for tier 3 (separator + weapon + armor + hint)
        if (tier.canShowEquipment()) {
            height += 36;
        }

        // Add padding for potion effects row if tier 3
        if (tier.canShowPotionEffects()) {
            height += 24;
        }

        return height;
    }

    /**
     * Renders the main fantasy-styled panel with tier-based stats.
     */
    private static void renderFantasyPanel(GuiGraphics graphics, EntityStats stats,
                                            int screenWidth, int screenHeight, float fade, Font font,
                                            GogglesTier tier) {

        int panelHeight = calculatePanelHeight(stats, tier);

        // Position (right side of screen, vertically centered)
        int panelX = screenWidth - PANEL_WIDTH - PANEL_RIGHT_MARGIN;
        int panelY = screenHeight / 2 - panelHeight / 2;

        // Clamp to screen bounds
        panelY = Math.max(panelY, 10);
        panelY = Math.min(panelY, screenHeight - panelHeight - 10);

        int alpha = (int) (fade * 255);

        // Draw panel background with gradient effect
        drawFantasyBackground(graphics, panelX, panelY, PANEL_WIDTH, panelHeight, alpha);

        // Draw decorative border
        drawFantasyBorder(graphics, panelX, panelY, PANEL_WIDTH, panelHeight, alpha);

        // Draw corner accents
        drawCornerAccents(graphics, panelX, panelY, PANEL_WIDTH, panelHeight, alpha);

        // Content positioning
        int contentX = panelX + 8;
        int contentY = panelY + 8;

        // === HEADER: Entity Name (centered, elegant) ===
        String name = stats.entityName();
        if (name.length() > 18) {
            name = name.substring(0, 16) + "..";
        }

        int nameWidth = font.width(name);
        int nameX = panelX + (PANEL_WIDTH - nameWidth) / 2;
        drawTextWithShadow(graphics, font, name, nameX, contentY, TEXT_NAME, alpha);

        contentY += 13;

        // === MOB LEVEL (from Auto-Leveling) - Tier 3 only ===
        if (tier.canShowMobLevel() && stats.hasMobLevel() && AutoLevelingIntegration.shouldShowMobLevel()) {
            String levelStr = "Lv. " + stats.getMobLevel();
            int levelWidth = font.width(levelStr);
            int levelX = panelX + (PANEL_WIDTH - levelWidth) / 2;
            drawTextWithShadow(graphics, font, levelStr, levelX, contentY, MOB_LEVEL_COLOR, alpha);
            contentY += 12;
        }

        // === Thin separator line ===
        drawSeparator(graphics, panelX + 10, contentY, PANEL_WIDTH - 20, alpha);
        contentY += 6;

        // === RUNE LEVEL (Power) - Available at all tiers ===
        if (tier.canShowRuneLevel()) {
            drawPowerLevel(graphics, font, stats, panelX, contentY, PANEL_WIDTH, alpha);
            contentY += 20;
        }

        // === STATS - Tier 2+ only ===
        if (tier.canShowStats()) {
            int labelX = contentX;

            // === COMBAT STATS - Portrait layout (two per row) ===
            // Row 1: HP and ATK
            drawStatLine(graphics, font, "HP", stats.getDisplayHealthString(),
                    labelX, contentY, HEALTH_COLOR, alpha);
            drawStatWithBonus(graphics, font, "ATK", stats.getDisplayBaseAttack(),
                    stats.getDisplayAttackBonus(), stats.hasAttackBonus(),
                    labelX + 60, contentY, ATTACK_COLOR, alpha);
            contentY += 12;

            // Row 2: DEF (if present) and ARM (if present)
            boolean hasRowTwo = stats.hasDefense() || stats.hasArmor();
            if (hasRowTwo) {
                int row2X = labelX;
                if (stats.hasDefense()) {
                    drawStatWithBonus(graphics, font, "DEF", stats.getDisplayBaseDefense(),
                            stats.getDisplayDefenseBonus(), stats.hasDefenseBonus(),
                            row2X, contentY, DEFENSE_COLOR, alpha);
                    row2X += 60;
                }

                if (stats.hasArmor()) {
                    drawStatLine(graphics, font, "ARM", String.valueOf(stats.getDisplayArmor()),
                            row2X, contentY, ARMOR_COLOR, alpha);
                }
                contentY += 12;
            }

            // Row 3: MAG (if present)
            if (stats.hasMagic()) {
                drawStatWithBonus(graphics, font, "MAG", stats.getDisplayBaseMagic(),
                        stats.getDisplayMagicBonus(), stats.hasMagicBonus(),
                        labelX, contentY, MAGIC_COLOR, alpha);
                contentY += 12;
            }
        }

        // === EQUIPMENT DISPLAY - Tier 3 only ===
        if (tier.canShowEquipment()) {
            LivingEntity target = EntityScanner.getInstance().getTarget();
            if (target != null) {
                contentY = drawEquipmentSection(graphics, font, target, panelX, contentX, contentY, PANEL_WIDTH, alpha);
            }
        }

        // === POTION EFFECT ICONS - Tier 3 only ===
        if (tier.canShowPotionEffects()) {
            LivingEntity target = EntityScanner.getInstance().getTarget();
            if (target != null) {
                drawEffectIcons(graphics, target, panelX, contentY, PANEL_WIDTH, alpha);
            }
        }
    }

    /**
     * Draws potion effect icons at the bottom of the panel.
     * Uses cached effect data from the server for other entities.
     */
    private static void drawEffectIcons(GuiGraphics graphics, LivingEntity entity,
                                         int panelX, int y, int panelWidth, int alpha) {
        Minecraft mc = Minecraft.getInstance();

        // Get effect data from cache (synced from server)
        EffectDataCache.EffectData effectData = EffectDataCache.getEffectData(entity.getId());

        // For local player, we can read effects directly
        boolean isLocalPlayer = mc.player != null && entity.getId() == mc.player.getId();

        List<EntityEffectsPacket.EffectEntry> effects;
        if (isLocalPlayer) {
            // Convert local player's effects to our format
            effects = entity.getActiveEffects().stream()
                    .map(effect -> {
                        var effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                        return effectId != null ? new EntityEffectsPacket.EffectEntry(effectId, effect.getAmplifier()) : null;
                    })
                    .filter(e -> e != null)
                    .toList();
        } else if (effectData != null && effectData.hasAnyEffects()) {
            effects = effectData.getAllEffects();
        } else {
            return; // No effects to display
        }

        if (effects.isEmpty()) {
            return;
        }

        MobEffectTextureManager textureManager = mc.getMobEffectTextures();

        // Calculate how many icons we can fit
        int maxIcons = (panelWidth - 16) / (EFFECT_ICON_SIZE + 2);
        int effectCount = Math.min(effects.size(), maxIcons);

        // Calculate starting X to center the icons
        int totalWidth = effectCount * EFFECT_ICON_SIZE + (effectCount - 1) * 2;
        int startX = panelX + (panelWidth - totalWidth) / 2;

        int iconX = startX;
        int iconIndex = 0;

        for (EntityEffectsPacket.EffectEntry effectEntry : effects) {
            if (iconIndex >= maxIcons) break;

            // Look up the MobEffect from the registry
            MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effectEntry.effectId());
            if (mobEffect == null) continue;

            TextureAtlasSprite sprite = textureManager.get(mobEffect);

            // Draw dark background for icon
            int bgAlpha = (int) (alpha * 0.6);
            graphics.fill(iconX - 1, y - 1, iconX + EFFECT_ICON_SIZE + 1, y + EFFECT_ICON_SIZE + 1,
                    applyAlpha(BG_DARK, bgAlpha));

            // Draw subtle border
            int borderAlpha = (int) (alpha * 0.4);
            graphics.fill(iconX - 1, y - 1, iconX + EFFECT_ICON_SIZE + 1, y, applyAlpha(BORDER_DARK, borderAlpha));
            graphics.fill(iconX - 1, y + EFFECT_ICON_SIZE, iconX + EFFECT_ICON_SIZE + 1, y + EFFECT_ICON_SIZE + 1, applyAlpha(BORDER_DARK, borderAlpha));
            graphics.fill(iconX - 1, y, iconX, y + EFFECT_ICON_SIZE, applyAlpha(BORDER_DARK, borderAlpha));
            graphics.fill(iconX + EFFECT_ICON_SIZE, y, iconX + EFFECT_ICON_SIZE + 1, y + EFFECT_ICON_SIZE, applyAlpha(BORDER_DARK, borderAlpha));

            // Draw the effect icon with alpha
            int iconAlpha = (int) (alpha * 0.9);
            graphics.setColor(1.0f, 1.0f, 1.0f, iconAlpha / 255.0f);
            graphics.blit(iconX, y, 0, EFFECT_ICON_SIZE, EFFECT_ICON_SIZE, sprite);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);  // Reset color

            iconX += EFFECT_ICON_SIZE + 2;
            iconIndex++;
        }
    }

    /**
     * Draws the equipment section showing weapon and armor.
     * Returns the new Y position after drawing.
     */
    private static int drawEquipmentSection(GuiGraphics graphics, Font font, LivingEntity entity,
                                             int panelX, int contentX, int y, int panelWidth, int alpha) {
        // Thin separator before equipment
        drawSeparator(graphics, panelX + 10, y, panelWidth - 20, alpha);
        y += 6;

        // Check main hand
        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!mainHand.isEmpty()) {
            String itemName = truncateString(mainHand.getHoverName().getString(), 14);
            drawStatLine(graphics, font, "WPN", itemName, contentX, y, ATTACK_COLOR, alpha);
            y += 10;
        }

        // Count armor pieces
        int armorCount = 0;
        StringBuilder armorStr = new StringBuilder();

        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = entity.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = entity.getItemBySlot(EquipmentSlot.FEET);

        if (!head.isEmpty()) armorCount++;
        if (!chest.isEmpty()) armorCount++;
        if (!legs.isEmpty()) armorCount++;
        if (!feet.isEmpty()) armorCount++;

        if (armorCount > 0) {
            // Show armor count and hint for snapshot
            String armorInfo = armorCount + "/4 pcs";
            drawStatLine(graphics, font, "GER", armorInfo, contentX, y, ARMOR_COLOR, alpha);
            y += 10;
        }

        // Hint for snapshot (double-tap)
        drawTextWithShadow(graphics, font, "[VV] Snapshot", contentX, y, TEXT_GRAY, (int)(alpha * 0.6));
        y += 10;

        return y;
    }

    /**
     * Truncates a string to a maximum length, adding ".." if truncated.
     */
    private static String truncateString(String str, int maxLen) {
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 2) + "..";
    }

    /**
     * Draws the dark background with Elden Ring aesthetic.
     * Uses 80% opacity for translucency.
     */
    private static void drawFantasyBackground(GuiGraphics graphics, int x, int y, int w, int h, int alpha) {
        // Main dark background - 80% opacity
        int bgAlpha = (int) (alpha * BG_OPACITY);
        graphics.fill(x, y, x + w, y + h, applyAlpha(BG_DARK, bgAlpha));

        // Inner panel (slightly lighter, also 80%)
        int innerMargin = 2;
        int innerAlpha = (int) (alpha * BG_OPACITY * 0.95);
        graphics.fill(x + innerMargin, y + innerMargin,
                x + w - innerMargin, y + h - innerMargin,
                applyAlpha(BG_INNER, innerAlpha));
    }

    /**
     * Draws the subtle border with Elden Ring style.
     */
    private static void drawFantasyBorder(GuiGraphics graphics, int x, int y, int w, int h, int alpha) {
        int x2 = x + w;
        int y2 = y + h;

        // Outer dark border
        int darkAlpha = (int) (alpha * 0.8);
        graphics.fill(x, y, x2, y + 1, applyAlpha(BORDER_DARK, darkAlpha));
        graphics.fill(x, y2 - 1, x2, y2, applyAlpha(BORDER_DARK, darkAlpha));
        graphics.fill(x, y, x + 1, y2, applyAlpha(BORDER_DARK, darkAlpha));
        graphics.fill(x2 - 1, y, x2, y2, applyAlpha(BORDER_DARK, darkAlpha));

        // Inner accent line (subtle gold)
        int accentAlpha = (int) (alpha * 0.5);
        graphics.fill(x + 1, y + 1, x2 - 1, y + 2, applyAlpha(BORDER_ACCENT, accentAlpha));
        graphics.fill(x + 1, y2 - 2, x2 - 1, y2 - 1, applyAlpha(BORDER_ACCENT, accentAlpha));
        graphics.fill(x + 1, y + 1, x + 2, y2 - 1, applyAlpha(BORDER_ACCENT, accentAlpha));
        graphics.fill(x2 - 2, y + 1, x2 - 1, y2 - 1, applyAlpha(BORDER_ACCENT, accentAlpha));
    }

    /**
     * Draws subtle corner accents (Elden Ring style).
     */
    private static void drawCornerAccents(GuiGraphics graphics, int x, int y, int w, int h, int alpha) {
        int accentAlpha = (int) (alpha * 0.6);
        int color = applyAlpha(BORDER_HIGHLIGHT, accentAlpha);

        // Small corner highlights
        int size = 4;

        // Top-left
        graphics.fill(x, y, x + size, y + 1, color);
        graphics.fill(x, y, x + 1, y + size, color);

        // Top-right
        graphics.fill(x + w - size, y, x + w, y + 1, color);
        graphics.fill(x + w - 1, y, x + w, y + size, color);

        // Bottom-left
        graphics.fill(x, y + h - 1, x + size, y + h, color);
        graphics.fill(x, y + h - size, x + 1, y + h, color);

        // Bottom-right
        graphics.fill(x + w - size, y + h - 1, x + w, y + h, color);
        graphics.fill(x + w - 1, y + h - size, x + w, y + h, color);
    }

    /**
     * Draws a thin separator line.
     */
    private static void drawSeparator(GuiGraphics graphics, int x, int y, int width, int alpha) {
        int lineAlpha = (int) (alpha * 0.4);
        graphics.fill(x, y, x + width, y + 1, applyAlpha(BORDER_ACCENT, lineAlpha));
    }

    /**
     * Draws the Power Level in Elden Ring style with bonus display.
     */
    private static void drawPowerLevel(GuiGraphics graphics, Font font, EntityStats stats,
                                       int panelX, int y, int panelWidth, int alpha) {
        // "Power Level" label - subtle
        String label = "Power Level";
        int labelWidth = font.width(label);
        int labelX = panelX + (panelWidth - labelWidth) / 2;
        drawTextWithShadow(graphics, font, label, labelX, y, TEXT_GRAY, alpha);

        // Build the power display string
        String powerStr;
        if (stats.hasPowerBonus()) {
            int bonus = stats.getDisplayPowerBonus();
            String bonusStr = bonus > 0 ? "+" + bonus : String.valueOf(bonus);
            powerStr = stats.getDisplayBasePowerLevel() + " (" + bonusStr + ")";
        } else {
            powerStr = String.valueOf(stats.getDisplayPowerLevel());
        }

        int powerWidth = font.width(powerStr);
        int powerX = panelX + (panelWidth - powerWidth) / 2;

        // Draw base power in gold
        if (stats.hasPowerBonus()) {
            String baseStr = String.valueOf(stats.getDisplayBasePowerLevel());
            drawTextWithShadow(graphics, font, baseStr, powerX, y + 9, RUNE_COLOR, alpha);

            // Draw bonus part
            int bonus = stats.getDisplayPowerBonus();
            String bonusStr = " (" + (bonus > 0 ? "+" + bonus : String.valueOf(bonus)) + ")";
            int bonusX = powerX + font.width(baseStr);
            int bonusColor = bonus > 0 ? BONUS_POSITIVE : BONUS_NEGATIVE;
            drawTextWithShadow(graphics, font, bonusStr, bonusX, y + 9, bonusColor, alpha);
        } else {
            drawTextWithShadow(graphics, font, powerStr, powerX, y + 9, RUNE_COLOR, alpha);
        }
    }

    /**
     * Draws the health bar with Elden Ring style.
     */
    private static void drawHealthBar(GuiGraphics graphics, Font font,
                                       String valueStr, double current, double max,
                                       int labelX, int barX, int y, int alpha) {
        // Draw label
        drawTextWithShadow(graphics, font, "HP", labelX, y, TEXT_GRAY, alpha);

        // Draw bar background (dark)
        int barBgAlpha = (int) (alpha * 0.7);
        graphics.fill(barX, y + 2, barX + BAR_WIDTH, y + 2 + BAR_HEIGHT,
                applyAlpha(0x0A0807, barBgAlpha));

        // Draw subtle bar border
        int borderAlpha = (int) (alpha * 0.4);
        graphics.fill(barX, y + 2, barX + BAR_WIDTH, y + 3, applyAlpha(BORDER_DARK, borderAlpha));
        graphics.fill(barX, y + 1 + BAR_HEIGHT, barX + BAR_WIDTH, y + 2 + BAR_HEIGHT, applyAlpha(BORDER_DARK, borderAlpha));

        // Calculate fill percentage
        double percentage = max > 0 ? Math.min(current / max, 1.0) : 0;
        int fillWidth = (int) (BAR_WIDTH * percentage);

        if (fillWidth > 0) {
            // Draw bar fill (solid crimson, Elden Ring style)
            int fillAlpha = (int) (alpha * 0.9);
            graphics.fill(barX, y + 2, barX + fillWidth, y + 2 + BAR_HEIGHT,
                    applyAlpha(HEALTH_COLOR, fillAlpha));

            // Subtle highlight at top
            int highlightAlpha = (int) (alpha * 0.3);
            graphics.fill(barX, y + 2, barX + fillWidth, y + 3,
                    applyAlpha(brightenColor(HEALTH_COLOR, 1.4), highlightAlpha));
        }

        // Draw value text at end of bar
        int valueX = barX + BAR_WIDTH + 5;
        drawTextWithShadow(graphics, font, valueStr, valueX, y, TEXT_LIGHT, alpha);
    }

    /**
     * Draws a stat line with label and muted value.
     */
    private static void drawStatLine(GuiGraphics graphics, Font font,
                                      String label, String value,
                                      int x, int y, int valueColor, int alpha) {
        // Draw label (muted)
        drawTextWithShadow(graphics, font, label, x, y, TEXT_GRAY, alpha);

        // Draw value with muted color
        int valueX = x + font.width(label) + 3;
        drawTextWithShadow(graphics, font, value, valueX, y, valueColor, alpha);
    }

    /**
     * Draws a stat line with bonus display (e.g., "ATK 5 +3").
     */
    private static void drawStatWithBonus(GuiGraphics graphics, Font font,
                                           String label, int baseValue, int bonus, boolean hasBonus,
                                           int x, int y, int valueColor, int alpha) {
        // Draw label (muted)
        drawTextWithShadow(graphics, font, label, x, y, TEXT_GRAY, alpha);

        int valueX = x + font.width(label) + 3;

        if (hasBonus) {
            // Draw base value
            String baseStr = String.valueOf(baseValue);
            drawTextWithShadow(graphics, font, baseStr, valueX, y, valueColor, alpha);

            // Draw bonus
            String bonusStr = bonus > 0 ? "+" + bonus : String.valueOf(bonus);
            int bonusX = valueX + font.width(baseStr) + 1;
            int bonusColor = bonus > 0 ? BONUS_POSITIVE : BONUS_NEGATIVE;
            drawTextWithShadow(graphics, font, bonusStr, bonusX, y, bonusColor, alpha);
        } else {
            // No bonus, just draw the value
            drawTextWithShadow(graphics, font, String.valueOf(baseValue), valueX, y, valueColor, alpha);
        }
    }

    /**
     * Draws text with a shadow effect.
     */
    private static void drawTextWithShadow(GuiGraphics graphics, Font font,
                                            String text, int x, int y, int color, int alpha) {
        int shadowAlpha = (int) (alpha * 0.5);
        int shadowColor = applyAlpha(0x000000, shadowAlpha);
        int mainColor = applyAlpha(color, alpha);

        // Shadow
        graphics.drawString(font, text, x + 1, y + 1, shadowColor, false);
        // Main text
        graphics.drawString(font, text, x, y, mainColor, false);
    }

    /**
     * Applies alpha to an RGB color.
     */
    private static int applyAlpha(int rgb, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    /**
     * Darkens a color by a factor.
     */
    private static int darkenColor(int color, double factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Brightens a color by a factor.
     */
    private static int brightenColor(int color, double factor) {
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }
}
