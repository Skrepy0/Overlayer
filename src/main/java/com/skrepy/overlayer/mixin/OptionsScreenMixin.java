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

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen) (Object) this;

        Button videoButton = overlayer$FindVideoSettingsButton(screen);
        if (videoButton == null) {
            return;
        }

        int buttonX = videoButton.getX() - 20 - 4 + Config.getOptionsScreenBtnXOffset();
        int buttonY = videoButton.getY() + Config.getOptionsScreenBtnYOffset();

        Button customButton = Button.builder(Component.literal("O"), (button) -> {
            Minecraft.getInstance().setScreenAndShow(new OverlayerSettingsScreen(screen));
        }).pos(buttonX, buttonY).size(20, 20).tooltip(Tooltip.create(Component.translatable("overlayer.screen.button.config.tooltip"))).build();

        overlayer$AddWidgetToScreen(screen, customButton);
    }

    @Unique
    private Button overlayer$FindVideoSettingsButton(Screen screen) {
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

    @Unique
    private void overlayer$AddWidgetToScreen(Screen screen, Button widget) {
        try {
            Field childrenField = Screen.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            ((List) childrenField.get(screen)).add(widget);

            Field renderablesField = Screen.class.getDeclaredField("renderables");
            renderablesField.setAccessible(true);
            ((List) renderablesField.get(screen)).add(widget);

            Field narratablesField = Screen.class.getDeclaredField("narratables");
            narratablesField.setAccessible(true);
            ((List) narratablesField.get(screen)).add(widget);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
