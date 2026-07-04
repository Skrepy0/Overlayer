package com.skrepy.overlayer.manager;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.data.OverlayerData;
import com.skrepy.overlayer.data.OverlayerDataManager;

import net.minecraft.network.chat.Component;

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
    }

    public void save() {
        OverlayerData data = new OverlayerData();
        data.setImageInstances(new ArrayList<>(instances));
        OverlayerDataManager.save(data);
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
                Component.translatable("overlayer.screen.common.select_pic").getString(), null, filterPatterns, null, false
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
