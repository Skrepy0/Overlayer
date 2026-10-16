package com.skrepy.overlayer.fabric;

import com.skrepy.overlayer.fabric.render.OverlayRenderer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

@SuppressWarnings("unused")
public class PlatformUtilsImpl {
    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static void invalidateSortCache() {
        OverlayRenderer.invalidateSortCache();
    }
}
