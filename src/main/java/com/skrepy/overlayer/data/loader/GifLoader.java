package com.skrepy.overlayer.data.loader;

import static com.skrepy.overlayer.data.loader.LoaderHelper.convertToNativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.icafe4j.image.gif.FrameReader;
import com.icafe4j.image.gif.GIFFrame;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

public class GifLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(GifLoader.class);
    private static final ExecutorService DECODER_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GIF-Decoder");
        t.setDaemon(true);
        return t;
    });

    private final String path;
    private final Path absolutePath;
    private final int id;

    // 加载完成后只读数据（由主线程访问）
    private volatile List<Identifier> gifTextures;
    private volatile List<Integer> gifDelays;
    private volatile int gifTotalDelay = 0;
    private volatile int originalWidth;
    private volatile int originalHeight;

    // 状态
    private volatile boolean gifLoaded = false;
    private volatile boolean gifLoading = false;

    // 播放辅助
    private long gifStartTime = 0;
    private int lastFrameIndex = -1;

    // 共享占位纹理（由主线程创建）
    private static Identifier placeholderTexture = null;

    public GifLoader(int id, String path, Path absolutePath) {
        this.id = id;
        this.path = path;
        this.absolutePath = absolutePath;
    }

    /**
     * 异步加载 GIF，解码在后台线程，纹理注册在主线程
     */
    public synchronized void loadAsync(TextureManager textureManager) {
        if (gifLoading || gifLoaded) return;
        if (absolutePath == null) {
            LOGGER.error("absolutePath is null, cannot load: id={}", id);
            return;
        }
        gifLoading = true;
        LOGGER.info("Starting async GIF load: id={}, path={}", id, path);

        CompletableFuture.supplyAsync(this::decodeFrames, DECODER_EXECUTOR).thenAcceptAsync(frameData -> {
            if (frameData == null || frameData.frames.isEmpty()) {
                LOGGER.warn("GIF decode failed or empty: id={}", id);
                gifLoading = false;
                return;
            }
            if (placeholderTexture == null) {
                placeholderTexture = createPlaceholderTexture(textureManager);
            }

            List<Identifier> textures = new ArrayList<>(frameData.frames.size());
            List<Integer> delays = new ArrayList<>(frameData.delays.size());
            int totalDelay = 0;

            for (int i = 0; i < frameData.frames.size(); i++) {
                BufferedImage frame = frameData.frames.get(i);
                int delay = frameData.delays.get(i);
                Identifier location;
                if (frame != null) {
                    NativeImage nativeImage = convertToNativeImage(frame);
                    DynamicTexture dynTex = new DynamicTexture(() -> "overlayer_gif", nativeImage);
                    location = Identifier.tryBuild("overlayer", "gif/" + UUID.randomUUID());
                    if (location == null) {
                        location = Identifier.withDefaultNamespace("gif/" + UUID.randomUUID());
                    }
                    textureManager.register(location, dynTex);
                } else {
                    location = placeholderTexture;
                    LOGGER.warn("Frame {} is null, using placeholder", i);
                }
                textures.add(location);
                delays.add(delay);
                totalDelay += delay;
            }

            gifTextures = textures;
            gifDelays = delays;
            gifTotalDelay = totalDelay;
            originalWidth = frameData.width;
            originalHeight = frameData.height;
            gifLoaded = true;
            gifLoading = false;
            gifStartTime = System.currentTimeMillis();
            LOGGER.info("GIF loaded async: id={}, frames={}, totalDelay={}ms", id, textures.size(), totalDelay);
        }, Minecraft.getInstance()).exceptionally(e -> {
            LOGGER.error("Async GIF load failed: id={}", id, e);
            gifLoading = false;
            return null;
        });
    }

    /**
     * 使用 FrameReader 逐帧解码
     */
    private FrameData decodeFrames() {
        FrameData result = new FrameData();
        File file = absolutePath.toFile();
        if (!file.exists() || !file.isFile()) {
            LOGGER.warn("File not exists: {}", path);
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            FrameReader reader = new FrameReader();
            GIFFrame gifFrame = reader.getGIFFrameEx(fis);
            if (gifFrame == null) {
                LOGGER.warn("No frame read from {}", path);
                return null;
            }

            boolean first = true;
            BufferedImage lastValidFrame = null;

            while (gifFrame != null) {
                BufferedImage frame = gifFrame.getFrame();
                int delay = gifFrame.getDelay() * 10;
                if (delay <= 0) delay = 100;

                // 帧为空时回退
                if (frame == null) {
                    if (lastValidFrame != null) {
                        BufferedImage copy = new BufferedImage(lastValidFrame.getWidth(), lastValidFrame.getHeight(), lastValidFrame.getType());
                        Graphics2D g = copy.createGraphics();
                        g.drawImage(lastValidFrame, 0, 0, null);
                        g.dispose();
                        frame = copy;
                        LOGGER.debug("Frame was null, replaced with previous frame");
                    } else {
                        LOGGER.error("First frame is null, aborting");
                        return null;
                    }
                }

                // 统一尺寸（缩放到第一帧大小）
                int w = frame.getWidth();
                int h = frame.getHeight();
                if (first) {
                    result.width = w;
                    result.height = h;
                    first = false;
                } else if (w != result.width || h != result.height) {
                    try {
                        BufferedImage scaled = new BufferedImage(result.width, result.height, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = scaled.createGraphics();
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g.drawImage(frame, 0, 0, result.width, result.height, null);
                        g.dispose();
                        frame = scaled;
                    } catch (Exception e) {
                        LOGGER.warn("Frame scaling failed: {}", e.getMessage());
                    }
                }

                lastValidFrame = frame;
                result.frames.add(frame);
                result.delays.add(delay);

                gifFrame = reader.getGIFFrameEx(fis);
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("ICAFE FrameReader decode error for {}", path, e);
            return null;
        }
    }

    @Nullable
    public synchronized Identifier getCurrentFrame() {
        if (!gifLoaded) return null;
        if (gifTextures == null || gifTextures.isEmpty()) return null;
        if (gifTotalDelay == 0) return gifTextures.getFirst();

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - gifStartTime;
        int cycleTime = (int) (elapsed % gifTotalDelay);
        int accum = 0;
        int selectedIndex = 0;
        for (int i = 0; i < gifDelays.size(); i++) {
            accum += gifDelays.get(i);
            if (cycleTime < accum) {
                selectedIndex = i;
                break;
            }
        }
        lastFrameIndex = selectedIndex;
        return gifTextures.get(selectedIndex);
    }

    private static synchronized Identifier createPlaceholderTexture(TextureManager textureManager) {
        if (placeholderTexture != null) return placeholderTexture;
        NativeImage placeholder = new NativeImage(NativeImage.Format.RGBA, 1, 1, false);
        placeholder.setPixelABGR(0, 0, 0x00000000);
        DynamicTexture dynTex = new DynamicTexture(() -> "overlayer_placeholder", placeholder);
        Identifier loc = Identifier.tryBuild("overlayer", "placeholder/" + UUID.randomUUID());
        if (loc == null) loc = Identifier.withDefaultNamespace("placeholder/" + UUID.randomUUID());
        textureManager.register(loc, dynTex);
        placeholderTexture = loc;
        LOGGER.debug("Placeholder texture created: {}", loc);
        return loc;
    }

    public void clearCache() {
        LOGGER.debug("Clearing GIF cache: id={}", id);
        gifTextures = null;
        gifDelays = null;
        gifLoaded = false;
        gifLoading = false;
        gifStartTime = 0;
        lastFrameIndex = -1;
    }

    public boolean isLoaded() {
        return gifLoaded;
    }

    public boolean isLoading() {
        return gifLoading;
    }

    public int getOriginalWidth() {
        return originalWidth;
    }

    public int getOriginalHeight() {
        return originalHeight;
    }

    public void resetStartTime() {
        this.gifStartTime = System.currentTimeMillis();
    }

    private static class FrameData {
        List<BufferedImage> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        int width, height;
    }
}
