package com.skrepy.overlayer.fabric;

import com.skrepy.overlayer.CommonOverlayer;
import net.fabricmc.api.ModInitializer;

import java.nio.file.Path;

import static com.mojang.text2speech.Narrator.LOGGER;


public class OverlayerFabric implements ModInitializer {
    public static Path GAME_DIR;
    @Override
    public void onInitialize() {
        CommonOverlayer.init();
        LOGGER.info("GAME_DIR:{}", GAME_DIR);
    }
}
