package com.skrepy.overlayer.data;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.ImageIO;
import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.data.loader.GifLoader;
import com.skrepy.overlayer.data.loader.LoaderHelper;
import com.skrepy.overlayer.data.loader.StaticImageLoader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;


public class ImageEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageEntry.class);
    private static final ExecutorService THUMBNAIL_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Thumbnail-Decoder");
        t.setDaemon(true);
        return t;
    });

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
    private transient volatile StaticImageLoader staticLoader;
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
        if (path == null || path.isEmpty()) return null;
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

    // 获取原始尺寸
    public int getOriginalWidth() {
        if (staticLoader != null && staticLoader.isLoaded()) {
            return staticLoader.getWidth();
        }
        if (gifLoader != null && gifLoader.isLoaded()) {
            return gifLoader.getOriginalWidth();
        }
        return 0;
    }

    public int getOriginalHeight() {
        if (staticLoader != null && staticLoader.isLoaded()) {
            return staticLoader.getHeight();
        }
        if (gifLoader != null && gifLoader.isLoaded()) {
            return gifLoader.getOriginalHeight();
        }
        return 0;
    }

    /**
     * 异步加载缩略图，加载完成后通过回调通知。
     *
     * @param textureManager 纹理管理器
     * @param onLoaded       加载完成回调（可 null），在主线程执行
     * @return 如果已缓存则立即返回纹理，否则返回 null（加载中/失败）
     */
    @Nullable
    public synchronized Identifier getThumbnail(TextureManager textureManager, @Nullable Runnable onLoaded) {
        if (thumbnailTexture != null) return thumbnailTexture;
        if (loadingThumbnail || thumbnailFailed) return null;

        loadingThumbnail = true;
        LOGGER.debug("Start async thumbnail load: id={}, path={}", id, path);

        CompletableFuture.supplyAsync(this::decodeThumbnail, THUMBNAIL_EXECUTOR).thenAcceptAsync(thumbnailImage -> {
            if (thumbnailImage == null) {
                LOGGER.warn("Thumbnail decode failed: id={}", id);
                loadingThumbnail = false;
                thumbnailFailed = true;
                return;
            }
            // 主线程注册纹理
            NativeImage nativeImage = LoaderHelper.convertToNativeImage(thumbnailImage);
            NativeImageBackedTexture dynTex = new NativeImageBackedTexture(nativeImage);
            Identifier location = Identifier.of("overlayer", "thumb/" + UUID.randomUUID());
            textureManager.registerTexture(location, dynTex);
            thumbnailTexture = location;
            loadingThumbnail = false;
            LOGGER.debug("Thumbnail loaded: id={}", id);
            // 执行回调
            if (onLoaded != null) {
                onLoaded.run();
            }
        }, MinecraftClient.getInstance()).exceptionally(e -> {
            LOGGER.error("Async thumbnail load failed: id={}", id, e);
            loadingThumbnail = false;
            thumbnailFailed = true;
            return null;
        });

        return null;
    }

    /**
     * 后台解码缩略图（同步方法，在后台线程执行）
     */
    @Nullable
    private BufferedImage decodeThumbnail() {
        try {
            Path absPath = getAbsolutePath();
            if (absPath == null) return null;
            File file = absPath.toFile();
            if (!file.exists() || !file.isFile()) return null;

            BufferedImage original = ImageIO.read(file);
            if (original == null) return null;

            int thumbSize = 64;
            BufferedImage scaled = new BufferedImage(thumbSize, thumbSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, thumbSize, thumbSize, null);
            g.dispose();
            return scaled;
        } catch (Exception e) {
            LOGGER.error("Thumbnail decode error: {}", path, e);
            return null;
        }
    }

    // ---------- 缓存清理 ----------
    public void clearCache() {
        LOGGER.debug("Clearing cache: id={}, path={}", id, path);
        if (staticLoader != null) {
            staticLoader.clearCache();
            staticLoader = null;
        }
        if (gifLoader != null) {
            gifLoader.clearCache();
            gifLoader = null;
        }
        thumbnailTexture = null;
        thumbnailFailed = false;
        loadingThumbnail = false;
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
                return gifLoader.getCurrentFrame();
            }
            return null;
        }

        if (staticLoader == null) {
            staticLoader = new StaticImageLoader(id, path, absPath);
        }
        return staticLoader.getOrLoad(textureManager);
    }

    private boolean isGifFile() {
        return path != null && path.toLowerCase().endsWith(".gif");
    }
}
