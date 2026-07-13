package com.skrepy.overlayer.mixin;

import com.skrepy.overlayer.render.OverlayRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Screen 的 Mixin 类。
 * <p>
 * 作用：用于在非游戏界面显示实例
 * </p>
 *
 * @author Skrepy
 * @since 1.0.0
 */
@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        OverlayRenderer.renderOverlays(guiGraphics, partialTick);
    }
}
