package com.skrepy.overlayer.neoforge;

import com.skrepy.overlayer.neoforge.render.OverlayRenderer;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

@SuppressWarnings("unused")
public class PlatformUtilsImpl {
    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static void invalidateSortCache() {
        OverlayRenderer.invalidateSortCache();
    }
}