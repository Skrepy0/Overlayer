package com.skrepy.overlayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.client.gui.OverlayerSettingsScreen;
import com.skrepy.overlayer.mixin.accessor.ScreenAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * 主菜单界面（TitleScreen）的 Mixin 类。
 * <p>
 * 作用：在主菜单的“辅助功能”按钮右侧添加一个自定义的 "O" 按钮，
 * 用于打开Overlayer的设置界面。
 * </p>
 *
 * @author Skrepy
 * @since 1.0.0
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (Minecraft.getInstance().isDemo()) {
            return;
        }

        TitleScreen screen = (TitleScreen) (Object) this;
        int buttonX = screen.width / 2 + 128 + Config.getTitleScreenBtnXOffset();
        int buttonY = screen.height / 4 + 138 + Config.getTitleScreenBtnYOffset();
        Button customButton = Button.builder(Component.literal("O"), (button) -> {
            Minecraft.getInstance().setScreen(new OverlayerSettingsScreen(screen));
        }).pos(buttonX, buttonY).size(20, 20).tooltip(Tooltip.create(Component.translatable("overlayer.screen.button.config.tooltip"))).build();

        ((ScreenAccessor) screen).invokeAddRenderableWidget(customButton);
    }
}
