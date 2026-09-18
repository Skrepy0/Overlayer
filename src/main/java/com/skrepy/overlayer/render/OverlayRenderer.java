package com.skrepy.overlayer.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class OverlayRenderer {
    private static List<ImageEntry> sortedCache = null;
    private static int lastSize = -1;
    private static int lastLayerVersion = -1;
    private static int layerVersion = 0;

    public static void invalidateSortCache() {
        sortedCache = null;
        layerVersion++;
    }

    public static void renderOverlays(GuiGraphicsExtractor gui, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        List<ImageEntry> instances = OverlayerManager.getInstance().getInstances();
        if (instances.isEmpty()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        TextureManager textureManager = mc.getTextureManager();

        if (sortedCache == null || instances.size() != lastSize || lastLayerVersion != layerVersion) {
            sortedCache = new ArrayList<>(instances);
            sortedCache.sort(Comparator.comparingInt(ImageEntry::getLayer));
            lastSize = instances.size();
            lastLayerVersion = layerVersion;
        }

        boolean inGame = mc.player != null && mc.level != null;

        for (ImageEntry entry : instances) {
            String mode = entry.getDisplayMode();
            if ("disabled".equals(mode)) continue;
            if ("ingame".equals(mode) && !inGame) continue;
            if ("not_ingame".equals(mode) && inGame) continue;

            Identifier texture = entry.getCurrentFrame(textureManager, partialTick);
            if (texture == null) {
                Overlayer.LOGGER.warn("纹理加载失败: {}", entry.getPath());
                continue;
            }

            int origWidth = entry.getOriginalWidth();
            int origHeight = entry.getOriginalHeight();
            if (origWidth <= 0 || origHeight <= 0) continue;

            double scale = entry.getScale();
            int drawWidth = (int) (origWidth * scale);
            int drawHeight = (int) (origHeight * scale);

            int rotation = entry.getRotation();

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
            int color = ARGB.color((int) (alpha * 255), 255, 255, 255);

            gui.pose().pushMatrix();
            gui.pose().translate(centerX, centerY);
            if (rotation != 0) {
                gui.pose().rotate((float) Math.toRadians(rotation));
            }
            gui.blit(
                    RenderPipelines.GUI_TEXTURED, texture, -drawWidth / 2, -drawHeight / 2, 0.0f, 0.0f, drawWidth, drawHeight, origWidth, origHeight, origWidth, origHeight, color
            );
            gui.pose().popMatrix();
        }
    }
}
