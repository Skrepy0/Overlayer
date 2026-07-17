package com.skrepy.overlayer;

import org.lwjgl.glfw.GLFW;

import com.skrepy.overlayer.render.OverlayRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class OverlayerClient implements ClientModInitializer {
    public static final KeyBinding TOGGLE_OVERLAY = new KeyBinding(
            "key.overlayer.toggle_show_status", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F1, "category.overlayer.general"
    );
    public static boolean overlayVisible = true;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(TOGGLE_OVERLAY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (TOGGLE_OVERLAY.wasPressed()) {
                overlayVisible = !overlayVisible;
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((screen1, drawContext, mouseX, mouseY, tickDelta) -> {
                if (overlayVisible) {
                    OverlayRenderer.renderOverlays(drawContext, tickDelta);
                }
            });
        });
    }
}
