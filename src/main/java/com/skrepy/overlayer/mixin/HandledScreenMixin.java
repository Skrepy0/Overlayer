package com.skrepy.overlayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.skrepy.overlayer.Config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext drawContext, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Config.getIsOpenContainerScreen()) {
            Config.setIsOpenContainerScreen(true);
        }
    }

    @Inject(method = "removed", at = @At("RETURN"))
    private void removed(CallbackInfo ci) {
        Config.setIsOpenContainerScreen(false);
    }
}
