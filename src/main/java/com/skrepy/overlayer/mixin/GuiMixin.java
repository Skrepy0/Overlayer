package com.skrepy.overlayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.render.OverlayRenderer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Gui 的 Mixin 类。
 * <p>
 * 作用：用于在游戏界面显示实例
 * </p>
 *
 * @author Skrepy
 * @since 1.0.0
 */
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(
            method = "extractRenderState", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void onExtractRenderState(
                                      DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci, ProfilerFiller profiler, int xMouse, int yMouse, GuiGraphicsExtractor graphics) {
        if (!Config.isIsOpenContainerScreen()) {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            OverlayRenderer.renderOverlays(graphics, partialTick);
        }
    }
}
