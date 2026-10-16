package com.skrepy.overlayer.fabric.mixin;

import com.skrepy.overlayer.fabric.Config;
import com.skrepy.overlayer.fabric.OverlayerClient;
import com.skrepy.overlayer.fabric.render.OverlayRenderer;
import com.skrepy.overlayer.manager.OverlayerManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!Config.getIsOpenContainerScreen()) {
            Config.setIsOpenContainerScreen(true);
        }
        if (OverlayerClient.overlayVisible && !OverlayerManager.getInstance().getInstances().isEmpty())
            OverlayRenderer.renderOverlays(graphics, a);
    }

    @Inject(method = "removed", at = @At("RETURN"))
    private void onRemoved(CallbackInfo ci) {
        Config.setIsOpenContainerScreen(false);
    }
}
