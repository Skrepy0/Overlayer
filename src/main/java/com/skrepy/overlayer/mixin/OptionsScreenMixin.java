package com.skrepy.overlayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.skrepy.overlayer.client.gui.OverlayerSettingsScreen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

@Environment(EnvType.CLIENT)
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen) (Object) this;

        ButtonWidget videoButton = findVideoSettingsButton(screen);
        if (videoButton == null) {
            return;
        }

        int buttonX = videoButton.getX() - 20 - 4;
        int buttonY = videoButton.getY();

        ButtonWidget customButton = ButtonWidget.builder(
                Text.literal("O"), (btn) -> MinecraftClient.getInstance().setScreen(new OverlayerSettingsScreen(screen))
        ).position(buttonX, buttonY).size(20, 20).tooltip(Tooltip.of(Text.translatable("overlayer.screen.button.config.tooltip"))).build();

        this.addDrawableChild(customButton);
    }

    @Unique
    private ButtonWidget findVideoSettingsButton(OptionsScreen screen) {
        return screen.children().stream().filter(ButtonWidget.class::isInstance).map(ButtonWidget.class::cast).filter(btn -> {
            Text message = btn.getMessage();
            if (message.getContent() instanceof TranslatableTextContent translatable) {
                return "options.video".equals(translatable.getKey());
            }
            return false;
        }).findFirst().orElse(null);
    }
}
