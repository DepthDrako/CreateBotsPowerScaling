package com.botzpowerscaling.client;

import com.botzpowerscaling.BotzPowerScaling;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Handles keybind registration for the power scanner.
 *
 * The scanner keybind must be held down to display the overlay.
 * Default key: V (can be changed in Controls menu under "BotzPowerScaling" category)
 */
public class KeyBindings {

    // Key category shown in the Controls menu
    public static final String KEY_CATEGORY = "key.categories.botzpowerscaling";

    // The scanner activation key
    public static KeyMapping SCANNER_KEY;

    /**
     * Creates and registers all keybindings.
     * Called during RegisterKeyMappingsEvent.
     */
    public static void register(RegisterKeyMappingsEvent event) {
        SCANNER_KEY = new KeyMapping(
                "key.botzpowerscaling.scanner",  // Translation key for the keybind name
                KeyConflictContext.IN_GAME,       // Only active when in-game (not in menus)
                InputConstants.Type.KEYSYM,       // Keyboard key (not mouse)
                GLFW.GLFW_KEY_V,                  // Default: V key
                KEY_CATEGORY                       // Category in Controls menu
        );

        event.register(SCANNER_KEY);

        BotzPowerScaling.LOGGER.info("Registered Power Scanner keybind (default: V)");
    }

    /**
     * Checks if the scanner key is currently being held down.
     *
     * @return true if the scanner activation key is pressed
     */
    public static boolean isScannerKeyHeld() {
        return SCANNER_KEY != null && SCANNER_KEY.isDown();
    }
}
