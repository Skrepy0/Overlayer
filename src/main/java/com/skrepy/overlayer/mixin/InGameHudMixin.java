package com.skrepy.overlayer.mixin;

import static com.skrepy.overlayer.OverlayerClient.overlayVisible;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.render.OverlayRenderer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (!Config.getIsOpenContainerScreen() && overlayVisible) {
            OverlayRenderer.renderOverlays(context, tickDelta);
        }
    }
}
