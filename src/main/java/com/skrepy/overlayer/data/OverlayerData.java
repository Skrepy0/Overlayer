package com.skrepy.overlayer.data;

import java.util.ArrayList;
import java.util.List;

public class OverlayerData {
    private List<ImageEntry> imageInstances = new ArrayList<>();
    private int titleScreenBtnXOffset;
    private int titleScreenBtnYOffset;
    private int optionsScreenBtnXOffset;
    private int optionsScreenBtnYOffset;

    public List<ImageEntry> getImageInstances() {
        return imageInstances;
    }

    public void setImageInstances(List<ImageEntry> imageInstances) {
        this.imageInstances = imageInstances;
    }

    public int getTitleScreenBtnXOffset() {
        return titleScreenBtnXOffset;
    }

    public void setTitleScreenBtnXOffset(int titleScreenBtnXOffset) {
        this.titleScreenBtnXOffset = titleScreenBtnXOffset;
    }

    public int getTitleScreenBtnYOffset() {
        return titleScreenBtnYOffset;
    }

    public void setTitleScreenBtnYOffset(int titleScreenBtnYOffset) {
        this.titleScreenBtnYOffset = titleScreenBtnYOffset;
    }

    public int getOptionsScreenBtnXOffset() {
        return optionsScreenBtnXOffset;
    }

    public void setOptionsScreenBtnXOffset(int optionsScreenBtnXOffset) {
        this.optionsScreenBtnXOffset = optionsScreenBtnXOffset;
    }

    public int getOptionsScreenBtnYOffset() {
        return optionsScreenBtnYOffset;
    }

    public void setOptionsScreenBtnYOffset(int optionsScreenBtnYOffset) {
        this.optionsScreenBtnYOffset = optionsScreenBtnYOffset;
    }
}
