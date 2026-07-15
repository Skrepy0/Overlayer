package com.skrepy.overlayer.mixin;

import java.lang.reflect.Field;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.client.gui.OverlayerSettingsScreen;
import com.skrepy.overlayer.mixin.accessor.ScreenAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * 设置界面（OptionsScreen）的 Mixin 类。
 * <p>
 * 作用：在选项的“视频设置”按钮左侧添加一个自定义的 "O" 按钮，
 * 用于打开Overlayer的设置界面。
 * </p>
 *
 * @author Skrepy
 * @since 1.0.0
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {

    @Unique
    private Button overlayer$customButton;

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen) (Object) this;

        overlayer$customButton = Button.builder(
                Component.literal("O"), btn -> Minecraft.getInstance().setScreen(new OverlayerSettingsScreen(screen))
        ).pos(0, 0).size(20, 20).tooltip(Tooltip.create(Component.translatable("overlayer.screen.button.config.tooltip"))).build();

        ((ScreenAccessor) screen).invokeAddRenderableWidget(overlayer$customButton);
        overlayer$updateCustomButtonPosition(screen);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void onRepositionElements(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen) (Object) this;
        overlayer$updateCustomButtonPosition(screen);
    }

    @Unique
    private void overlayer$updateCustomButtonPosition(OptionsScreen screen) {
        if (overlayer$customButton == null) return;

        Button videoButton = overlayer$findVideoSettingsButton(screen);
        if (videoButton == null) return;

        int newX = videoButton.getX() - 20 - 4 + Config.getOptionsScreenBtnXOffset();
        int newY = videoButton.getY() + Config.getOptionsScreenBtnYOffset();

        overlayer$customButton.setX(newX);
        overlayer$customButton.setY(newY);
    }

    @Unique
    private Button overlayer$findVideoSettingsButton(Screen screen) {
        try {
            Field childrenField = Screen.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            List<?> children = (List<?>) childrenField.get(screen);

            for (Object child : children) {
                if (child instanceof Button button) {
                    Component message = button.getMessage();
                    if (message instanceof MutableComponent mutable) {
                        Object contents = mutable.getContents();
                        if (contents instanceof TranslatableContents translatable && "options.video".equals(translatable.getKey())) {
                            return button;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
