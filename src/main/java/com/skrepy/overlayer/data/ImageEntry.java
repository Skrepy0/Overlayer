package com.skrepy.overlayer.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public class ImageEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageEntry.class);

    // 持久化字段
    private int id;
    private String path;
    private int xOffset;          // -200 ~ 200 百分比
    private int yOffset;          // -200 ~ 200 百分比
    private String displayMode;   // "always", "ingame", "not_ingame", "disabled"
    private double scale;         // 0.01 ~ 3.0
    private double alpha;         // 0.01 ~ 1.0
    private int layer;            // 非负整数，图层优先级

    // 纹理缓存
    private transient volatile ResourceLocation originalTexture;
    private transient volatile int originalWidth;
    private transient volatile int originalHeight;
    private transient volatile boolean loadingOriginal = false;
    private transient volatile boolean originalFailed = false;

    // 缩略图缓存（用于列表）
    private transient volatile ResourceLocation thumbnailTexture;
    private transient volatile boolean loadingThumbnail = false;
    private transient volatile boolean thumbnailFailed = false;

    // 构造函数
    public ImageEntry() {
    }

    public ImageEntry(int id, String path) {
        this(id, path, 0, 0, "always", 1.0, 1.0, 0);
    }

    public ImageEntry(int id, String path, int xOffset, int yOffset, String displayMode, double scale, double alpha, int layer) {
        this.id = id;
        this.path = path;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.displayMode = displayMode;
        this.scale = scale;
        this.alpha = alpha;
        this.layer = layer;
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
        return new File(path).getName();
    }

    public void clearCache() {
        originalTexture = null;
        thumbnailTexture = null;
        originalFailed = false;
        thumbnailFailed = false;
        loadingOriginal = false;
        loadingThumbnail = false;
    }

    // ---------- 纹理加载 ----------
    /**
     * 获取原图纹理（全分辨率），并记录宽高
     */
    @Nullable
    public synchronized ResourceLocation getOriginalTexture(TextureManager textureManager) {
        if (originalTexture != null) return originalTexture;
        if (loadingOriginal || originalFailed) return null;

        loadingOriginal = true;
        try {
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                LOGGER.warn("图片文件不存在: {}", path);
                originalFailed = true;
                loadingOriginal = false;
                return null;
            }

            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                LOGGER.warn("无法读取图片（文件格式不支持）: {}", path);
                originalFailed = true;
                loadingOriginal = false;
                return null;
            }

            originalWidth = image.getWidth();
            originalHeight = image.getHeight();

            // 转换为 NativeImage (RGBA)
            NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, originalWidth, originalHeight, false);
            for (int y = 0; y < originalHeight; y++) {
                for (int x = 0; x < originalWidth; x++) {
                    int argb = image.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    nativeImage.setPixelRGBA(x, y, abgr);
                }
            }

            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            ResourceLocation location = ResourceLocation.tryBuild("overlayer", "images/" + UUID.randomUUID());
            if (location == null) {
                location = ResourceLocation.withDefaultNamespace("images/" + UUID.randomUUID());
            }
            textureManager.register(location, dynamicTexture);
            originalTexture = location;
            LOGGER.debug("加载原图成功: {} ({}x{})", path, originalWidth, originalHeight);
            loadingOriginal = false;
            return originalTexture;
        } catch (Exception e) {
            LOGGER.error("加载原图失败: {}", path, e);
            originalFailed = true;
            loadingOriginal = false;
            return null;
        }
    }

    /**
     * 获取原图宽度（需先加载）
     */
    public int getOriginalWidth() {
        return originalWidth;
    }

    /**
     * 获取原图高度（需先加载）
     */
    public int getOriginalHeight() {
        return originalHeight;
    }

    /**
     * 获取缩略图（64x64），用于列表
     */
    @Nullable
    public synchronized ResourceLocation getThumbnail(TextureManager textureManager) {
        if (thumbnailTexture != null) return thumbnailTexture;
        if (loadingThumbnail || thumbnailFailed) return null;

        loadingThumbnail = true;
        try {
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                thumbnailFailed = true;
                loadingThumbnail = false;
                return null;
            }

            BufferedImage original = ImageIO.read(file);
            if (original == null) {
                thumbnailFailed = true;
                loadingThumbnail = false;
                return null;
            }

            int thumbSize = 64;
            BufferedImage scaled = new BufferedImage(thumbSize, thumbSize, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, thumbSize, thumbSize, null);
            g.dispose();

            NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, thumbSize, thumbSize, false);
            for (int y = 0; y < thumbSize; y++) {
                for (int x = 0; x < thumbSize; x++) {
                    int argb = scaled.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g0 = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    int abgr = (a << 24) | (b << 16) | (g0 << 8) | r;
                    nativeImage.setPixelRGBA(x, y, abgr);
                }
            }

            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            ResourceLocation location = ResourceLocation.tryBuild("overlayer", "thumb/" + UUID.randomUUID());
            if (location == null) {
                location = ResourceLocation.withDefaultNamespace("thumb/" + UUID.randomUUID());
            }
            textureManager.register(location, dynamicTexture);
            thumbnailTexture = location;
            loadingThumbnail = false;
            return thumbnailTexture;
        } catch (Exception e) {
            LOGGER.error("加载缩略图失败: {}", path, e);
            thumbnailFailed = true;
            loadingThumbnail = false;
            return null;
        }
    }

    /**
     * 获取全尺寸纹理（用于渲染和预览），返回原图
     * 参数 targetSize 被忽略，为了兼容旧调用保留
     */
    @Nullable
    public ResourceLocation getFullTexture(TextureManager textureManager, int targetSize) {
        return getOriginalTexture(textureManager);
    }
}
