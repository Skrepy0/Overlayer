package com.skrepy.overlayer.data;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public class ImageEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageEntry.class);

    // 持久化字段
    private int id;
    private String path;
    private int xOffset;
    private int yOffset;
    private String displayMode;
    private double scale;
    private double alpha;
    private int layer;

    // 静态纹理缓存
    private transient volatile ResourceLocation staticTexture;
    private transient volatile int originalWidth;
    private transient volatile int originalHeight;
    private transient volatile boolean loadingStatic = false;
    private transient volatile boolean staticFailed = false;

    // GIF 数据
    private transient volatile List<ResourceLocation> gifTextures;
    private transient volatile List<Integer> gifDelays;
    private transient volatile int gifTotalDelay = 0;
    private transient volatile long gifStartTime = 0;
    private transient volatile boolean gifLoaded = false;
    private transient volatile boolean gifLoading = false;
    private transient volatile boolean isGif = false;

    // 缩略图
    private transient volatile ResourceLocation thumbnailTexture;
    private transient volatile boolean loadingThumbnail = false;
    private transient volatile boolean thumbnailFailed = false;

    public ImageEntry(int id, String path) {
        this(id, path, 0, 0, "disabled", 1.0, 1.0, 0);
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
        staticTexture = null;
        gifTextures = null;
        gifDelays = null;
        thumbnailTexture = null;
        staticFailed = false;
        gifLoaded = false;
        gifLoading = false;
        thumbnailFailed = false;
        loadingStatic = false;
        loadingThumbnail = false;
        isGif = false;
        gifStartTime = 0;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    // ---------- 纹理加载 ----------
    @Nullable
    public synchronized ResourceLocation getCurrentFrame(TextureManager textureManager, float partialTick) {
        if (isGif && !gifLoaded) {
            if (!gifLoading) {
                loadGif(textureManager);
            }
            return gifLoaded && !gifTextures.isEmpty() ? getCurrentGifFrame() : null;
        }

        if (staticTexture != null) return staticTexture;
        if (loadingStatic || staticFailed) return null;

        // 检查是否为 GIF
        if (path.toLowerCase().endsWith(".gif")) {
            isGif = true;
            loadGif(textureManager);
            return gifLoaded && !gifTextures.isEmpty() ? getCurrentGifFrame() : null;
        }

        loadStatic(textureManager);
        return staticTexture;
    }

    // ---------- GIF 加载 ----------
    private synchronized void loadGif(TextureManager textureManager) {
        if (gifLoading || gifLoaded) return;
        gifLoading = true;
        try {
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                LOGGER.warn("GIF 文件不存在: {}", path);
                gifLoading = false;
                loadStatic(textureManager);
                return;
            }

            try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                if (!readers.hasNext()) {
                    LOGGER.warn("未找到 GIF 读取器: {}", path);
                    gifLoading = false;
                    loadStatic(textureManager);
                    return;
                }
                ImageReader reader = readers.next();
                reader.setInput(stream);

                int numFrames = reader.getNumImages(true);
                if (numFrames <= 0) {
                    LOGGER.warn("GIF 无帧: {}", path);
                    gifLoading = false;
                    loadStatic(textureManager);
                    return;
                }

                List<ResourceLocation> textures = new ArrayList<>();
                List<Integer> delays = new ArrayList<>();
                int totalDelay = 0;

                for (int i = 0; i < numFrames; i++) {
                    BufferedImage frame = reader.read(i);
                    if (frame == null) continue;

                    // 精确获取帧延迟（毫秒）
                    int delay = getFrameDelay(reader, i);
                    if (delay <= 0) delay = 100; // 默认 100ms

                    // 转换为 NativeImage 并注册纹理
                    NativeImage nativeImage = convertToNativeImage(frame);
                    DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                    ResourceLocation location = ResourceLocation.tryBuild("overlayer", "gif/" + UUID.randomUUID());
                    if (location == null) location = ResourceLocation.withDefaultNamespace("gif/" + UUID.randomUUID());
                    textureManager.register(location, dynamicTexture);
                    textures.add(location);
                    delays.add(delay);
                    totalDelay += delay;

                    if (i == 0) {
                        originalWidth = frame.getWidth();
                        originalHeight = frame.getHeight();
                    }
                }

                if (textures.isEmpty()) {
                    LOGGER.warn("GIF 无有效帧: {}", path);
                    gifLoading = false;
                    loadStatic(textureManager);
                    return;
                }

                gifTextures = textures;
                gifDelays = delays;
                gifTotalDelay = totalDelay;
                gifLoaded = true;
                gifStartTime = System.currentTimeMillis();
                LOGGER.info("GIF 加载成功: {} ({} 帧, 总延迟 {}ms)", path, textures.size(), totalDelay);
            }
        } catch (Exception e) {
            LOGGER.error("GIF 加载失败: {}", path, e);
            loadStatic(textureManager);
        } finally {
            gifLoading = false;
        }
    }

    /**
     * 精确解析 GIF 帧延迟时间（单位：毫秒）
     */
    private int getFrameDelay(ImageReader reader, int frameIndex) {
        try {
            IIOMetadata metadata = reader.getImageMetadata(frameIndex);
            if (metadata == null) return 0;

            // 使用标准 GIF 元数据格式
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree("javax_imageio_gif_image_1.0");
            if (root == null) return 0;

            // 查找 GraphicControlExtension 节点
            NodeList nodes = root.getElementsByTagName("GraphicControlExtension");
            if (nodes.getLength() > 0) {
                IIOMetadataNode node = (IIOMetadataNode) nodes.item(0);
                String delayStr = node.getAttribute("delayTime");
                if (delayStr != null && !delayStr.isEmpty()) {
                    int delay = Integer.parseInt(delayStr);
                    // delayTime 单位是 1/100 秒，转换为毫秒
                    return delay * 10;
                }
            }

            // 备用方法：遍历子节点（兼容某些 GIF 格式）
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if ("GraphicControlExtension".equals(child.getNodeName())) {
                    NamedNodeMap attrs = child.getAttributes();
                    Node delayNode = attrs.getNamedItem("delayTime");
                    if (delayNode != null) {
                        int delay = Integer.parseInt(delayNode.getNodeValue());
                        return delay * 10;
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败，返回 0，调用者使用默认值
            LOGGER.debug("解析帧延迟失败，使用默认值: {}", path);
        }
        return 0;
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
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }
        return nativeImage;
    }

    @Nullable
    private ResourceLocation getCurrentGifFrame() {
        if (gifTextures == null || gifTextures.isEmpty()) return null;
        if (gifTotalDelay == 0) return gifTextures.get(0);

        long elapsed = System.currentTimeMillis() - gifStartTime;
        int cycleTime = (int) (elapsed % gifTotalDelay);
        int accum = 0;
        for (int i = 0; i < gifDelays.size(); i++) {
            accum += gifDelays.get(i);
            if (cycleTime < accum) {
                return gifTextures.get(i);
            }
        }
        return gifTextures.get(0);
    }

    // ---------- 静态图片加载 ----------
    private void loadStatic(TextureManager textureManager) {
        if (loadingStatic || staticFailed) return;
        loadingStatic = true;
        try {
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                LOGGER.warn("静态图片不存在: {}", path);
                staticFailed = true;
                loadingStatic = false;
                return;
            }
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                LOGGER.warn("无法读取静态图片: {}", path);
                staticFailed = true;
                loadingStatic = false;
                return;
            }
            originalWidth = image.getWidth();
            originalHeight = image.getHeight();

            NativeImage nativeImage = convertToNativeImage(image);
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            ResourceLocation location = ResourceLocation.tryBuild("overlayer", "img/" + UUID.randomUUID());
            if (location == null) location = ResourceLocation.withDefaultNamespace("img/" + UUID.randomUUID());
            textureManager.register(location, dynamicTexture);
            staticTexture = location;
            LOGGER.debug("静态图片加载成功: {} ({}x{})", path, originalWidth, originalHeight);
        } catch (Exception e) {
            LOGGER.error("静态图片加载失败: {}", path, e);
            staticFailed = true;
        } finally {
            loadingStatic = false;
        }
    }

    // ---------- 缩略图 ----------
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

            NativeImage nativeImage = convertToNativeImage(scaled);
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
            ResourceLocation location = ResourceLocation.tryBuild("overlayer", "thumb/" + UUID.randomUUID());
            if (location == null) location = ResourceLocation.withDefaultNamespace("thumb/" + UUID.randomUUID());
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
}
