package com.skrepy.overlayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.render.OverlayRenderer;

import net.minecraft.client.gui.GuiGraphicsExtractor;

@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Config.isIsOpenContainerScreen()) {
            Config.setIsOpenContainerScreen(true);
        }
        OverlayRenderer.renderOverlays(graphics, partialTick);
    }

    @Inject(method = "removed", at = @At("RETURN"))
    private void onRemoved(CallbackInfo ci) {
        Config.setIsOpenContainerScreen(false);
    }
}
