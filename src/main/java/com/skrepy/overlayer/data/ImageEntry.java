package com.skrepy.overlayer.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.NativeImage;
import com.skrepy.overlayer.Overlayer;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public class ImageEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageEntry.class);

    // 持久化字段
    private int id;
    private String path;
    private int xOffset;
    private int yOffset;
    private int rotation;
    private String displayMode;
    private double scale;
    private double alpha;
    private int layer;

    // ---------- transient 缓存 ----------
    private transient volatile Identifier staticTexture;
    private transient volatile int originalWidth;
    private transient volatile int originalHeight;
    private transient volatile boolean loadingStatic = false;
    private transient volatile boolean staticFailed = false;

    private transient volatile GifLoader gifLoader;

    private transient volatile Identifier thumbnailTexture;
    private transient volatile boolean loadingThumbnail = false;
    private transient volatile boolean thumbnailFailed = false;

    // ---------- 构造方法 ----------
    public ImageEntry(int id, String path) {
        this(id, path, 0, 0, "disabled", 1.0, 1.0, 0);
    }

    public ImageEntry(int id, String path, int xOffset, int yOffset, String displayMode, double scale, double alpha, int layer) {
        this.id = id;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.displayMode = displayMode;
        this.scale = scale;
        this.alpha = alpha;
        this.layer = layer;
        this.path = path;
        LOGGER.debug("ImageEntry created: id={}, path={}", id, path);
    }

    // ---------- Getter / Setter ----------
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
        clearCache();
        LOGGER.debug("Path updated: id={}, newPath={}", id, path);
    }

    public Path getAbsolutePath() {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return Overlayer.GAME_DIR.resolve(path).normalize();
    }

    public int getXOffset() {
        return xOffset;
    }

    public void setXOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public void setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public String getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(String displayMode) {
        this.displayMode = displayMode;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public String getDisplayName() {
        Path abs = getAbsolutePath();
        return abs != null ? abs.toFile().getName() : path;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    // ---------- 缓存清理 ----------
    public void clearCache() {
        LOGGER.debug("Clearing cache: id={}, path={}", id, path);
        staticTexture = null;
        thumbnailTexture = null;
        staticFailed = false;
        thumbnailFailed = false;
        loadingStatic = false;
        loadingThumbnail = false;
        if (gifLoader != null) {
            gifLoader.clearCache();
            gifLoader = null;
        }
    }

    // ---------- 主纹理获取 ----------
    @Nullable
    public synchronized Identifier getCurrentFrame(TextureManager textureManager, float partialTick) {
        Path absPath = getAbsolutePath();
        if (absPath == null) {
            LOGGER.warn("Cannot get absolute path: id={}, path={}", id, path);
            return null;
        }

        if (isGifFile()) {
            if (gifLoader == null) {
                gifLoader = new GifLoader(id, path, absPath);
            }
            if (!gifLoader.isLoaded() && !gifLoader.isLoading()) {
                gifLoader.loadAsync(textureManager);
            }
            if (gifLoader.isLoaded()) {
                Identifier frame = gifLoader.getCurrentFrame();
                if (frame != null) {
                    if (originalWidth == 0) {
                        originalWidth = gifLoader.getOriginalWidth();
                        originalHeight = gifLoader.getOriginalHeight();
                    }
                    return frame;
                }
            }
            return null; // 加载中或失败
        }

        // 静态图片
        if (staticTexture != null) {
            return staticTexture;
        }
        if (loadingStatic || staticFailed) {
            return null;
        }
        loadStatic(textureManager);
        return staticTexture;
    }

    private boolean isGifFile() {
        return path != null && path.toLowerCase().endsWith(".gif");
    }

    // ---------- 静态图片加载 ----------
    private void loadStatic(TextureManager textureManager) {
        if (loadingStatic || staticFailed) return;
        LOGGER.debug("Loading static image: id={}, path={}", id, path);
        long start = System.currentTimeMillis();
        loadingStatic = true;
        try {
            Path absPath = getAbsolutePath();
            if (absPath == null) {
                staticFailed = true;
                return;
            }
            File file = absPath.toFile();
            if (!file.exists() || !file.isFile()) {
                staticFailed = true;
                return;
            }
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                staticFailed = true;
                return;
            }
            originalWidth = image.getWidth();
            originalHeight = image.getHeight();

            NativeImage nativeImage = convertToNativeImage(image);
            DynamicTexture dynTex = new DynamicTexture(() -> "overlayer_texture", nativeImage);
            Identifier location = Identifier.tryBuild("overlayer", "img/" + UUID.randomUUID());
            if (location == null) location = Identifier.withDefaultNamespace("img/" + UUID.randomUUID());
            textureManager.register(location, dynTex);
            staticTexture = location;
            LOGGER.debug("Static image loaded in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            LOGGER.error("Failed to load static image", e);
            staticFailed = true;
        } finally {
            loadingStatic = false;
        }
    }

    // ---------- 缩略图 ----------
    @Nullable
    public synchronized Identifier getThumbnail(TextureManager textureManager) {
        if (thumbnailTexture != null) return thumbnailTexture;
        if (loadingThumbnail || thumbnailFailed) return null;

        LOGGER.debug("Loading thumbnail: id={}, path={}", id, path);
        long start = System.currentTimeMillis();
        loadingThumbnail = true;
        try {
            Path absPath = getAbsolutePath();
            if (absPath == null) {
                thumbnailFailed = true;
                return null;
            }
            File file = absPath.toFile();
            if (!file.exists() || !file.isFile()) {
                thumbnailFailed = true;
                return null;
            }
            BufferedImage original = ImageIO.read(file);
            if (original == null) {
                thumbnailFailed = true;
                return null;
            }

            int thumbSize = 64;
            BufferedImage scaled = new BufferedImage(thumbSize, thumbSize, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, thumbSize, thumbSize, null);
            g.dispose();

            NativeImage nativeImage = convertToNativeImage(scaled);
            DynamicTexture dynTex = new DynamicTexture(() -> "overlayer_thumb", nativeImage);
            Identifier location = Identifier.tryBuild("overlayer", "thumb/" + UUID.randomUUID());
            if (location == null) location = Identifier.withDefaultNamespace("thumb/" + UUID.randomUUID());
            textureManager.register(location, dynTex);
            thumbnailTexture = location;
            LOGGER.debug("Thumbnail loaded in {}ms", System.currentTimeMillis() - start);
            loadingThumbnail = false;
            return thumbnailTexture;
        } catch (Exception e) {
            LOGGER.error("Failed to load thumbnail", e);
            thumbnailFailed = true;
            loadingThumbnail = false;
            return null;
        }
    }

    // ---------- 工具方法 ----------
    private NativeImage convertToNativeImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelABGR(x, y, abgr);
            }
        }
        return nativeImage;
    }
}
