package com.skrepy.overlayer.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.skrepy.overlayer.fabric.render.OverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;


public class OverlayerClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("overlayer", "general"));

    public static final KeyMapping TOGGLE_OVERLAY = new KeyMapping(
            "key.overlayer.toggle_show_status", InputConstants.Type.KEYBOARD, GLFW.GLFW_KEY_F1, CATEGORY
    );

    public static boolean overlayVisible = true;

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(TOGGLE_OVERLAY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_OVERLAY.consumeClick()) {
                overlayVisible = !overlayVisible;
            }
        });

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterExtract(screen).register(
                    (screen1, graphics, mouseX, mouseY, tickProgress) -> {
                        if (overlayVisible) {
                            OverlayRenderer.renderOverlays(graphics, tickProgress);
                        }
                    });
        });
    }
}
