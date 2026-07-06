package com.skrepy.overlayer;

import com.skrepy.overlayer.manager.OverlayerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

import java.util.List;

public class Overlayer implements ModInitializer {
    public static final String MOD_ID = "overlayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final List<String> validFormat = List.of("png", "jpg", "jpeg", "bmp", "gif");
    @Override
    public void onInitialize() {
        OverlayerManager.getInstance().load();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
