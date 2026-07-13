package com.skrepy.overlayer.mixin;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.render.OverlayRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
public class AbstractContainerScreen {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Config.isIsOpenContainerScreen()) {
            Config.setIsOpenContainerScreen(true);
        }
        OverlayRenderer.renderOverlays(guiGraphics, partialTick);
    }

    @Inject(method = "removed", at = @At("RETURN"))
    private void removed(CallbackInfo ci) {
        Config.setIsOpenContainerScreen(false);
    }
}
