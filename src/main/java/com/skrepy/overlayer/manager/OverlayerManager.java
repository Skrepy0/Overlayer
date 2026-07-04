package com.skrepy.overlayer.manager;

import java.util.ArrayList;
import java.util.List;

import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.data.OverlayerData;
import com.skrepy.overlayer.data.OverlayerDataManager;

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

    public void addInstance(ImageEntry entry) {
        instances.add(entry);
        save();
    }

    public void removeInstance(ImageEntry entry) {
        instances.remove(entry);
        save();
    }

    public void updateInstance(ImageEntry entry) {
        // 直接修改引用即可，保存数据
        save();
    }
}
