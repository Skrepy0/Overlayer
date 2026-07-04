package com.skrepy.overlayer.client.render;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public class OverlayRenderer {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        List<ImageEntry> instances = OverlayerManager.getInstance().getInstances();
        if (instances.isEmpty()) return;

        TextureManager textureManager = mc.getTextureManager();

        for (ImageEntry entry : instances) {
            if ("disabled".equals(entry.getDisplayMode())) continue;

            ResourceLocation texture = entry.getOriginalTexture(textureManager);
            if (texture == null) {
                continue;
            }

            int originalWidth = entry.getOriginalWidth();
            int originalHeight = entry.getOriginalHeight();
            if (originalWidth <= 0 || originalHeight <= 0) continue;

            double scale = entry.getScale();
            int drawWidth = (int) (originalWidth * scale);
            int drawHeight = (int) (originalHeight * scale);

            double xOffsetPercent = entry.getXOffset() / 200.0;
            double yOffsetPercent = entry.getYOffset() / 200.0;
            int offsetX = (int) (screenWidth * xOffsetPercent);
            int offsetY = (int) (screenHeight * yOffsetPercent);
            int centerX = screenWidth / 2 + offsetX;
            int centerY = screenHeight / 2 + offsetY;

            float alpha = (float) entry.getAlpha();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
            guiGraphics.blit(texture, centerX - drawWidth / 2, centerY - drawHeight / 2, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }
}
