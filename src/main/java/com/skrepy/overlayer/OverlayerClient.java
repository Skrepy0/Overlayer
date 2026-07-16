package com.skrepy.overlayer;

import com.skrepy.overlayer.render.OverlayRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class OverlayerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((screen1, drawContext, mouseX, mouseY, tickDelta) -> {
                OverlayRenderer.renderOverlays(drawContext, tickDelta);
            });
        });
    }
}
