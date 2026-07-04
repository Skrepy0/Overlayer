package com.skrepy.overlayer.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        // 只有在游戏窗口有焦点时才渲染（可选）
        // if (!mc.isWindowActive()) return;

        List<ImageEntry> instances = OverlayerManager.getInstance().getInstances();
        if (instances.isEmpty()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        TextureManager textureManager = mc.getTextureManager();

        // 按图层排序（数值大的在上层）
        instances.sort((a, b) -> Integer.compare(a.getLayer(), b.getLayer()));

        for (ImageEntry entry : instances) {
            if ("disabled".equals(entry.getDisplayMode())) continue;

            // 模式过滤
            boolean inGame = mc.player != null && mc.level != null;
            String mode = entry.getDisplayMode();
            if ("ingame".equals(mode) && !inGame) continue;
            if ("not_ingame".equals(mode) && inGame) continue;
            // "always" 始终显示

            ResourceLocation texture = entry.getFullTexture(textureManager, 256);
            if (texture == null) {
                Overlayer.LOGGER.warn("纹理加载失败: {}", entry.getPath());
                continue;
            }

            // 计算位置（偏移百分比）
            double xOffsetPercent = entry.getXOffset() / 200.0;
            double yOffsetPercent = entry.getYOffset() / 200.0;
            int offsetX = (int) (screenWidth * xOffsetPercent);
            int offsetY = (int) (screenHeight * yOffsetPercent);
            int centerX = screenWidth / 2 + offsetX;
            int centerY = screenHeight / 2 + offsetY;

            // 缩放
            double scale = entry.getScale();
            int drawWidth = (int) (256 * scale);
            int drawHeight = (int) (256 * scale);

            // 透明度
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
