package com.skrepy.overlayer.data.loader;

import java.awt.image.BufferedImage;

import com.mojang.blaze3d.platform.NativeImage;

public class LoaderHelper {
    private LoaderHelper() {
    }

    public static NativeImage convertToNativeImage(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }
        return nativeImage;
    }
}
