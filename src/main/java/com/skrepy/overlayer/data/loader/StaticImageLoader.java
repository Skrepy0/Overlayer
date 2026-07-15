package com.skrepy.overlayer.data.loader;

import static com.skrepy.overlayer.data.loader.LoaderHelper.convertToNativeImage;

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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

/**
 * 异步加载静态图片，使用 icafe4j 解码。
 */
public class StaticImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticImageLoader.class);

    private static final ExecutorService DECODER_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "StaticImage-Decoder");
        t.setDaemon(true);
        return t;
    });

    private final int id;
    private final String path;
    private final Path absolutePath;

    private volatile Identifier texture;
    private volatile int width;
    private volatile int height;
    private volatile boolean loaded = false;
    private volatile boolean loading = false;
    private volatile boolean failed = false;

    public StaticImageLoader(int id, String path, Path absolutePath) {
        this.id = id;
        this.path = path;
        this.absolutePath = absolutePath;
    }

    /**
     * 获取纹理标识符。
     * - 若已加载完成，返回纹理。
     * - 若正在加载或已失败，返回 null。
     * - 若未开始加载，则启动异步加载，返回 null。
     */
    @Nullable
    public synchronized Identifier getOrLoad(TextureManager textureManager) {
        if (loaded && texture != null) {
            return texture;
        }
        if (loading || failed) {
            return null;
        }
        // 未加载 → 启动异步加载
        loadAsync(textureManager);
        return null;
    }

    /**
     * 异步加载图片，纹理注册将在主线程完成。
     */
    public synchronized void loadAsync(TextureManager textureManager) {
        if (loading || loaded) return;
        if (absolutePath == null) {
            LOGGER.error("absolutePath is null, cannot load: id={}", id);
            failed = true;
            return;
        }
        loading = true;
        LOGGER.debug("Start async loading static image: id={}, path={}", id, path);

        CompletableFuture.supplyAsync(this::decodeImage, DECODER_EXECUTOR).thenAcceptAsync(imageData -> {
            if (imageData == null) {
                LOGGER.warn("Decode failed for: id={}, path={}", id, path);
                loading = false;
                failed = true;
                return;
            }
            // 主线程注册纹理
            NativeImage nativeImage = imageData.nativeImage;
            NativeImageBackedTexture dynTex = new NativeImageBackedTexture(() -> "overlayer_texture", nativeImage);
            Identifier location = Identifier.of("overlayer", "img/" + UUID.randomUUID());
            textureManager.registerTexture(location, dynTex);
            texture = location;
            width = imageData.width;
            height = imageData.height;
            loaded = true;
            loading = false;
            LOGGER.debug("Static image loaded: id={}, size={}x{}", id, width, height);
        }, MinecraftClient.getInstance()).exceptionally(e -> {
            LOGGER.error("Async static image load failed: id={}", id, e);
            loading = false;
            failed = true;
            return null;
        });
    }

    /**
     * 后台解码，返回 NativeImage 和尺寸。
     */
    @Nullable
    private ImageData decodeImage() {
        try {
            File file = absolutePath.toFile();
            if (!file.exists() || !file.isFile()) {
                LOGGER.warn("File not exists: {}", file.getAbsolutePath());
                return null;
            }
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                LOGGER.warn("ImageIO.read returned null for: {}", file.getAbsolutePath());
                return null;
            }
            NativeImage nativeImage = convertToNativeImage(image);
            return new ImageData(nativeImage, image.getWidth(), image.getHeight());
        } catch (Exception e) {
            LOGGER.error("Decode error: {}", absolutePath, e);
            return null;
        }
    }

    public void clearCache() {
        texture = null;
        width = 0;
        height = 0;
        loaded = false;
        loading = false;
        failed = false;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isFailed() {
        return failed;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private record ImageData(NativeImage nativeImage, int width, int height) {
    }
}
