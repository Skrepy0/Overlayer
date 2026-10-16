package com.skrepy.overlayer;

import com.mojang.logging.LogUtils;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.data.loader.GifLoader;
import com.skrepy.overlayer.data.loader.StaticImageLoader;
import com.skrepy.overlayer.manager.OverlayerManager;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;

import static com.skrepy.overlayer.PlatformUtils.getGameDir;

public final class CommonOverlayer {
    public static final String MOD_ID = "overlayer";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final List<String> validFormat = List.of("png", "jpg", "jpeg", "bmp", "tif", "tiff", "ico", "pcx", "gif");
    public static Path GAME_DIR;

    private static void registerShutdownHook() {
        GAME_DIR = getGameDir();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            StaticImageLoader.shutdownExecutor();
            GifLoader.shutdownExecutor();
            ImageEntry.shutdownExecutor();
        }, "CommonOverlayer-Shutdown"));
    }

    public static void init() {
        LOGGER.info("GAME_DIR:{}", GAME_DIR);
        OverlayerManager.getInstance().load();
        registerShutdownHook();
    }
}
