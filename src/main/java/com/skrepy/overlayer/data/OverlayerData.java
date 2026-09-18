package com.skrepy.overlayer.data;

import java.util.ArrayList;
import java.util.List;

public class OverlayerData {
    private List<ImageEntry> imageInstances = new ArrayList<>();

    public List<ImageEntry> getImageInstances() {
        return imageInstances;
    }

    public void setImageInstances(List<ImageEntry> imageInstances) {
        this.imageInstances = imageInstances;
    }
}
