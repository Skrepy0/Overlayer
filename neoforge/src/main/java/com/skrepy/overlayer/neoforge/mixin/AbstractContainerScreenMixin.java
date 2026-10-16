package com.skrepy.overlayer.neoforge.mixin;

import com.skrepy.overlayer.manager.OverlayerManager;
import com.skrepy.overlayer.neoforge.Config;
import com.skrepy.overlayer.neoforge.OverlayerClient;
import com.skrepy.overlayer.neoforge.render.OverlayRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!Config.isIsOpenContainerScreen()) {
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
