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
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

@Environment(EnvType.CLIENT)
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    @Unique
    private Button overlayer$customButton;

    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void overlayer$onInit(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen) (Object) this;

        Button videoButton = overlayer$findVideoSettingsButton(screen);
        if (videoButton == null) {
            return;
        }

        overlayer$customButton = Button.builder(
                Component.literal("O"), (btn) -> {
                    boolean shiftDown = InputConstants.isKeyDown(
                            Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT
                    );
                    if (shiftDown) {
                        Minecraft.getInstance().gui.setScreen(new ConfigScreen());
                    } else {
                        Minecraft.getInstance().gui.setScreen(new OverlayerSettingsScreen(screen));
                    }
                }
        ).bounds(0, 0, 20, 20).tooltip(Tooltip.create(Component.translatable("overlayer.screen.button.config.tooltip"))).build();

        this.addRenderableWidget(overlayer$customButton);
        overlayer$updateCustomButtonPosition(screen);
    }

    @Unique
    private Button overlayer$findVideoSettingsButton(OptionsScreen screen) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(btn -> {
            Component message = btn.getMessage();
            if (message.getContents() instanceof TranslatableContents translatable) {
                return "options.video".equals(translatable.getKey());
            }
            return false;
        }).findFirst().orElse(null);
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

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void overlayer$onRepositionElements(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen) (Object) this;
        overlayer$updateCustomButtonPosition(screen);
    }
}
