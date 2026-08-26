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
        // Bulk read all pixels at once to minimize JNI boundary crossings
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);

        // Convert ARGB to ABGR using incrementing counters instead of modulo/division
        int x = 0, y = 0;
        for (int argb : pixels) {
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            nativeImage.setPixelABGR(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            x++;
            if (x >= w) {
                x = 0;
                y++;
            }
        }
        return nativeImage;
    }
}
