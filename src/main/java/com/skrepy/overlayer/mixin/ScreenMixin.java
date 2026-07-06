package com.skrepy.overlayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.skrepy.overlayer.render.OverlayRenderer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext drawContext, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        OverlayRenderer.renderOverlays(drawContext, partialTick);
    }
}
