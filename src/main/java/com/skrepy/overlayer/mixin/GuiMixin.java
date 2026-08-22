package com.skrepy.overlayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.manager.OverlayerManager;
import com.skrepy.overlayer.render.OverlayRenderer;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

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

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!Config.isIsOpenContainerScreen() && !OverlayerManager.getInstance().getInstances().isEmpty()) {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            OverlayRenderer.renderOverlays(guiGraphics, partialTick);
        }
    }
}
