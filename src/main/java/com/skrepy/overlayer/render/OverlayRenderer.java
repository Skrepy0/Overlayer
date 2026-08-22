package com.skrepy.overlayer.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public class OverlayRenderer {

    private static List<ImageEntry> sortedCache = null;
    private static int lastSize = -1;
    private static int lastLayerVersion = -1;
    private static int layerVersion = 0;

    public static void invalidateSortCache() {
        sortedCache = null;
        layerVersion++;
    }

    public static void renderOverlays(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        List<ImageEntry> instances = OverlayerManager.getInstance().getInstances();
        if (instances.isEmpty()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        TextureManager textureManager = mc.getTextureManager();

        // Only re-sort when list size or layer configuration changes
        if (sortedCache == null || instances.size() != lastSize || lastLayerVersion != layerVersion) {
            sortedCache = new ArrayList<>(instances);
            sortedCache.sort(Comparator.comparingInt(ImageEntry::getLayer));
            lastSize = instances.size();
            lastLayerVersion = layerVersion;
        }

        // Determine if in game
        boolean inGame = mc.player != null && mc.level != null;

        // Enable blend once before the loop
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (ImageEntry entry : sortedCache) {
            // Mode filtering
            String mode = entry.getDisplayMode();
            if ("disabled".equals(mode)) continue;
            if ("ingame".equals(mode) && !inGame) continue;
            if ("not_ingame".equals(mode) && inGame) continue;

            // Get current frame texture
            ResourceLocation texture = entry.getCurrentFrame(textureManager, partialTick);
            if (texture == null) {
                Overlayer.LOGGER.warn("纹理加载失败: {}", entry.getPath());
                continue;
            }

            int origWidth = entry.getOriginalWidth();
            int origHeight = entry.getOriginalHeight();
            if (origWidth <= 0 || origHeight <= 0) continue;

            // Scale
            double scale = entry.getScale();
            int drawWidth = (int) (origWidth * scale);
            int drawHeight = (int) (origHeight * scale);

            // Skip rotation when 0 to avoid unnecessary quaternion multiplication
            int rotation = entry.getRotation();

            // Calculate position
            double xOffsetPercent = entry.getXOffset() / 200.0;
            double yOffsetPercent = entry.getYOffset() / 200.0;
            int offsetX = (int) (screenWidth * xOffsetPercent);
            int offsetY = (int) (screenHeight * yOffsetPercent);
            int centerX = screenWidth / 2 + offsetX;
            int centerY = screenHeight / 2 + offsetY;

            // Screen bounds culling: skip if entirely off-screen
            int left = centerX - drawWidth / 2;
            int right = left + drawWidth;
            int top = centerY - drawHeight / 2;
            int bottom = top + drawHeight;
            if (right < 0 || left > screenWidth || bottom < 0 || top > screenHeight) {
                continue;
            }

            float alpha = (float) entry.getAlpha();

            guiGraphics.setColor(1.0f, 1.0f, 1.0f, alpha);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY, 0);
            if (rotation != 0) {
                guiGraphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
            }
            guiGraphics.blit(texture, -drawWidth / 2, -drawHeight / 2, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
            guiGraphics.pose().popPose();
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        // Disable blend once after the loop
        RenderSystem.disableBlend();
    }
}
