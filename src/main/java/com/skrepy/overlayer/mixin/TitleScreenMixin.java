package com.skrepy.overlayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.client.gui.ConfigScreen;
import com.skrepy.overlayer.client.gui.OverlayerSettingsScreen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.isDemo()) {
            return;
        }

        TitleScreen screen = (TitleScreen) (Object) this;
        int buttonX = screen.width / 2 + 128 + Config.getTitleScreenBtnXOffset();
        int buttonY = screen.height / 4 + 132 + Config.getTitleScreenBtnYOffset();

        ButtonWidget customButton = ButtonWidget.builder(
                Text.literal("O"), (btn) -> {
                    if (Screen.hasShiftDown()) {
                        MinecraftClient.getInstance().setScreen(new ConfigScreen());
                    } else {
                        MinecraftClient.getInstance().setScreen(new OverlayerSettingsScreen(screen));
                    }
                }
        ).position(buttonX, buttonY).size(20, 20).tooltip(Tooltip.of(Text.translatable("overlayer.screen.button.config.tooltip"))).build();

        addDrawableChild(customButton);
    }
}
