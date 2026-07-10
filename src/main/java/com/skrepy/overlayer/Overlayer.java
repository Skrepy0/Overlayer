package com.skrepy.overlayer;

import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skrepy.overlayer.manager.OverlayerManager;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class Overlayer implements ModInitializer {
    public static final String MOD_ID = "overlayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static Path GAME_DIR;
    public static final List<String> validFormat = List.of("png", "jpg", "jpeg", "bmp", "gif");

    @Override
    public void onInitialize() {
        GAME_DIR = FabricLoader.getInstance().getGameDir();
        LOGGER.info("GAME_DIR:{}", GAME_DIR);
        OverlayerManager.getInstance().load();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
