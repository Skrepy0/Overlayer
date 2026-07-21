package com.skrepy.overlayer;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Overlayer.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Overlayer.MOD_ID, value = Dist.CLIENT)
public class OverlayerClient {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("overlayer", "general"));
    public static final KeyMapping TOGGLE_OVERLAY = new KeyMapping(
            "key.overlayer.toggle_show_status", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F1, CATEGORY
    );
    public static boolean overlayVisible = true;

    public OverlayerClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (TOGGLE_OVERLAY.consumeClick()) {
            overlayVisible = !overlayVisible;
        }
    }

    @SubscribeEvent
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(TOGGLE_OVERLAY);
    }
}
