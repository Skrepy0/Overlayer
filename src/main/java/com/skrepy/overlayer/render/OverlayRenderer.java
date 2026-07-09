package com.skrepy.overlayer.render;

import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

public class OverlayRenderer {

    public static void renderOverlays(DrawContext drawContext, float partialTick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<ImageEntry> instances = OverlayerManager.getInstance().getInstances();
        if (instances.isEmpty()) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        TextureManager textureManager = mc.getTextureManager();

        // 按图层排序（数值大的在上层）
        instances.sort(Comparator.comparingInt(ImageEntry::getLayer));

        // 判断是否在游戏中
        boolean inGame = mc.player != null && mc.world != null;

        for (ImageEntry entry : instances) {
            // 模式过滤
            String mode = entry.getDisplayMode();
            if ("disabled".equals(mode)) continue;
            if ("ingame".equals(mode) && !inGame) continue;
            if ("not_ingame".equals(mode) && inGame) continue;

            // 获取当前帧纹理
            Identifier texture = entry.getCurrentFrame(textureManager, partialTick);
            if (texture == null) {
                Overlayer.LOGGER.warn("纹理加载失败: {}", entry.getPath());
                continue;
            }

            int origWidth = entry.getOriginalWidth();
            int origHeight = entry.getOriginalHeight();
            if (origWidth <= 0 || origHeight <= 0) continue;

            // 缩放
            double scale = entry.getScale();
            int drawWidth = (int) (origWidth * scale);
            int drawHeight = (int) (origHeight * scale);

            // 偏移
            double xOffsetPercent = entry.getXOffset() / 200.0;
            double yOffsetPercent = entry.getYOffset() / 200.0;
            int offsetX = (int) (screenWidth * xOffsetPercent);
            int offsetY = (int) (screenHeight * yOffsetPercent);
            int centerX = screenWidth / 2 + offsetX;
            int centerY = screenHeight / 2 + offsetY;

            float alpha = (float) entry.getAlpha();

            int color = ColorHelper.getArgb((int) (alpha * 255), 255, 255, 255);
            drawContext.drawTexture(RenderLayer::getGuiTextured, texture, centerX - drawWidth / 2, centerY - drawHeight / 2, 0.0f, 0.0f, drawWidth, drawHeight, drawWidth, drawHeight, color);
        }
    }
}
