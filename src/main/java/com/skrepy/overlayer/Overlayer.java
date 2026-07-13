package com.skrepy.overlayer;

import com.mojang.logging.LogUtils;
import com.skrepy.overlayer.manager.OverlayerManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;

@Mod(Overlayer.MOD_ID)
public class Overlayer {
    public static final String MOD_ID = "overlayer";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final List<String> validFormat = List.of("png", "jpg", "jpeg", "bmp", "gif");
    public static Path GAME_DIR;

    public Overlayer(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        GAME_DIR = FMLPaths.GAMEDIR.get();
        LOGGER.info("GAME_DIR:{}", GAME_DIR);
        OverlayerManager.getInstance().load();
    }

    @SubscribeEvent
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info(MOD_ID + " common setup");
    }
}
