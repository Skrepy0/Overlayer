package com.skrepy.overlayer;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class PlatformUtils {
    @ExpectPlatform
    public static Path getGameDir() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void invalidateSortCache() {
        throw new AssertionError();
    }
}