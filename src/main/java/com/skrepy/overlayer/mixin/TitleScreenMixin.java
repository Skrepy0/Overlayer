package com.skrepy.overlayer.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.InputConstants;
import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.client.gui.ConfigScreen;
import com.skrepy.overlayer.client.gui.OverlayerSettingsScreen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * 主菜单界面（TitleScreen）的 Mixin 类。
 * <p>
 * 作用：在主菜单“辅助功能”按钮的右侧添加一个自定义 "O" 按钮，
 * 用于打开 Overlayer 的设置界面。
 * </p>
 *
 * @author Skrepy
 * @since 1.0.0
 */
@Environment(EnvType.CLIENT)
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private Button overlayer$customButton;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void overlayer$onInit(CallbackInfo ci) {
        if (Minecraft.getInstance().isDemo()) {
            return;
        }

        TitleScreen screen = (TitleScreen) (Object) this;
        Button accessibilityButton = overlayer$findAccessibilityButton(screen);
        if (accessibilityButton == null) {
            return;
        }

        overlayer$customButton = Button.builder(
                Component.literal("O"), (_) -> {
                    boolean shiftDown = InputConstants.isKeyDown(
                            Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT
                    );
                    if (shiftDown) {
                        Minecraft.getInstance().gui.setScreen(new ConfigScreen(screen));
                    } else {
                        Minecraft.getInstance().gui.setScreen(new OverlayerSettingsScreen(screen));
                    }
                }
        ).bounds(0, 0, 20, 20).tooltip(Tooltip.create(Component.translatable("overlayer.screen.button.config.tooltip"))).build();

        // 相对辅助功能按钮定位，并叠加配置偏移
        overlayer$customButton.setX(
                accessibilityButton.getX() + accessibilityButton.getWidth() + 82 + Config.getTitleScreenBtnXOffset());
        overlayer$customButton.setY(
                accessibilityButton.getY() + Config.getTitleScreenBtnYOffset() + 24);

        // 一次注册，自动加入 children / renderables / narratables
        this.addRenderableWidget(overlayer$customButton);
    }

    @Unique
    private Button overlayer$findAccessibilityButton(TitleScreen screen) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(btn -> {
            if (btn.getMessage().getContents() instanceof TranslatableContents translatable) {
                return "options.accessibility".equals(translatable.getKey());
            }
            return false;
        }).findFirst().orElse(null);
    }
}
