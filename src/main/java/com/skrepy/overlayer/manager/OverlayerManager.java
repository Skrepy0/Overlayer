package com.skrepy.overlayer.manager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.data.OverlayerData;
import com.skrepy.overlayer.data.OverlayerDataManager;
import com.skrepy.overlayer.render.OverlayRenderer;

import net.minecraft.network.chat.Component;

public class OverlayerManager {
    private static final OverlayerManager INSTANCE = new OverlayerManager();
    private final List<ImageEntry> instances = new ArrayList<>();

    private OverlayerManager() {
    }

    public static OverlayerManager getInstance() {
        return INSTANCE;
    }

    public static String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        fileName = fileName.substring(fileName.lastIndexOf('\\') + 1); // Windows 兼容
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    public static boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    public static String selectFile(MemoryStack stack) {
        PointerBuffer filterPatterns = stack.mallocPointer(Overlayer.validFormat.size() + 1);
        for (String format : Overlayer.validFormat) {
            String pattern = "*." + format;
            filterPatterns.put(stack.UTF8(pattern));
        }
        filterPatterns.flip();

        return TinyFileDialogs.tinyfd_openFileDialog(
                Component.translatable("overlayer.screen.common.select_pic").getString(), null, filterPatterns, null, false
        );
    }

    public List<ImageEntry> getInstances() {
        return instances;
    }

    public void load() {
        instances.clear();
        OverlayerData data = OverlayerDataManager.load();
        instances.addAll(data.getImageInstances());
    }

    public void save() {
        OverlayerData data = new OverlayerData();
        data.setImageInstances(new ArrayList<>(instances));
        OverlayerDataManager.save(data);
    }

    public void addInstance(ImageEntry entry) {
        instances.add(entry);
        OverlayRenderer.invalidateSortCache();
        save();
    }

    public void removeInstance(ImageEntry entry) {
        instances.remove(entry);
        OverlayRenderer.invalidateSortCache();
        save();
    }

    public void updateInstance(ImageEntry entry) {
        OverlayRenderer.invalidateSortCache();
        save();
    }
}
