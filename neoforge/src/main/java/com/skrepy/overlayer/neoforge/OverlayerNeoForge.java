package com.skrepy.overlayer.neoforge;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

import java.util.List;

@Mod(OverlayerNeoForge.MOD_ID)
public class OverlayerNeoForge {
    public static final String MOD_ID = "overlayer";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final List<String> validFormat = List.of("png", "jpg", "jpeg", "bmp", "tif", "tiff", "ico", "pcx", "gif");

    public OverlayerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info(MOD_ID + " common setup");
    }
}
