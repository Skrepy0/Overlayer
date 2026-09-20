package com.skrepy.overlayer.data;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skrepy.overlayer.Overlayer;

public class OverlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "overlayer_data.json";
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Overlayer-DataSaver");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean savePending = new AtomicBoolean(false);
    private static final ReentrantLock saveLock = new ReentrantLock();
    private static volatile OverlayerData pendingData;

    private static Path getDataPath() {
        return Paths.get(".", "config", "overlayer", DATA_FILE);
    }

    public static OverlayerData load() {
        Path path = getDataPath();
        if (Files.notExists(path)) {
            Overlayer.LOGGER.info("配置文件不存在，使用默认数据");
            return new OverlayerData();
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            OverlayerData data = GSON.fromJson(reader, OverlayerData.class);
            return data != null ? data : new OverlayerData();
        } catch (IOException e) {
            Overlayer.LOGGER.error("加载配置文件失败", e);
            return new OverlayerData();
        }

    }

    public static void save(OverlayerData data) {
        pendingData = data;
        if (!savePending.compareAndSet(false, true)) {
            return; // Another save is already queued
        }
        SAVE_EXECUTOR.execute(() -> {
            try {
                saveLock.lock();
                performSave(pendingData);
            } finally {
                saveLock.unlock();
                savePending.set(false);
            }
        });
    }

    private static void performSave(OverlayerData data) {
        Path path = getDataPath();
        try {
            Files.createDirectories(path.getParent());
            String json = GSON.toJson(data);
            Overlayer.LOGGER.debug("保存数据: JSON长度={}, 路径={}", json.length(), path.toAbsolutePath());
            // Atomic write: write to temp file, then rename
            Path tempFile = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tempFile, json);
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Overlayer.LOGGER.debug("数据已保存到 {}", path);
        } catch (IOException e) {
            Overlayer.LOGGER.error("保存配置文件失败", e);
        }
    }

    public static void shutdown() {
        SAVE_EXECUTOR.shutdown();
        try {
            if (!SAVE_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                SAVE_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            SAVE_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
