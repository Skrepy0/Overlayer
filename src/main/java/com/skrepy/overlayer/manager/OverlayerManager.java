package com.skrepy.overlayer.manager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.data.OverlayerData;
import com.skrepy.overlayer.data.OverlayerDataManager;

import net.minecraft.text.Text;

public class OverlayerManager {
    private static final OverlayerManager INSTANCE = new OverlayerManager();
    private final List<ImageEntry> instances = new ArrayList<>();

    private OverlayerManager() {
    }

    public static OverlayerManager getInstance() {
        return INSTANCE;
    }

    public List<ImageEntry> getInstances() {
        return instances;
    }

    public void load() {
        instances.clear();
        OverlayerData data = OverlayerDataManager.load();
        instances.addAll(data.getImageInstances());
        Config.setTitleScreenBtnXOffset(data.getTitleScreenBtnXOffset());
        Config.setTitleScreenBtnYOffset(data.getTitleScreenBtnYOffset());
        Config.setOptionsScreenBtnXOffset(data.getOptionsScreenBtnXOffset());
        Config.setOptionsScreenBtnYOffset(data.getOptionsScreenBtnYOffset());
    }

    public void save() {
        OverlayerData data = new OverlayerData();
        data.setImageInstances(new ArrayList<>(instances));
        data.setTitleScreenBtnXOffset(Config.getTitleScreenBtnXOffset());
        data.setTitleScreenBtnYOffset(Config.getTitleScreenBtnYOffset());
        data.setOptionsScreenBtnXOffset(Config.getOptionsScreenBtnXOffset());
        data.setOptionsScreenBtnYOffset(Config.getOptionsScreenBtnYOffset());
        OverlayerDataManager.save(data);
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
        PointerBuffer filterPatterns = stack.mallocPointer(5);
        filterPatterns.put(stack.UTF8("*.png"));
        filterPatterns.put(stack.UTF8("*.jpg"));
        filterPatterns.put(stack.UTF8("*.jpeg"));
        filterPatterns.put(stack.UTF8("*.bmp"));
        filterPatterns.put(stack.UTF8("*.gif"));
        filterPatterns.flip();

        return TinyFileDialogs.tinyfd_openFileDialog(
                Text.translatable("overlayer.screen.common.select_pic").getString(), null, filterPatterns, null, false
        );
    }

    public void addInstance(ImageEntry entry) {
        instances.add(entry);
        save();
    }

    public void removeInstance(ImageEntry entry) {
        instances.remove(entry);
        save();
    }

    public void updateInstance(ImageEntry entry) {
        save();
    }
}
