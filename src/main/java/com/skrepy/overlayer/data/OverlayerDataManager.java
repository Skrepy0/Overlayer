package com.skrepy.overlayer.data;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skrepy.overlayer.Overlayer;

public class OverlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "overlayer_data.json";

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
        Path path = getDataPath();
        try {
            Files.createDirectories(path.getParent());
            String json = GSON.toJson(data);
            // 打印路径长度和JSON长度，便于调试
            Overlayer.LOGGER.debug("保存数据: JSON长度={}, 路径={}", json.length(), path.toAbsolutePath());
            // 使用 Files.writeString 确保完整写入
            Files.writeString(path, json);
            Overlayer.LOGGER.debug("数据已保存到 {}", path);
        } catch (IOException e) {
            Overlayer.LOGGER.error("保存配置文件失败", e); // 打印堆栈
        }
    }
}
