package com.skrepy.overlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class Overlayer implements ModInitializer {
    public static final String MOD_ID = "overlayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
