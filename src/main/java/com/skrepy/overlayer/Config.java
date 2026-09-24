package com.skrepy.overlayer;

import com.skrepy.overlayer.data.OverlayerData;

public class Config {
    private static boolean isOpenContainerScreen;
    private static int titleScreenBtnXOffset;
    private static int titleScreenBtnYOffset;
    private static int optionsScreenBtnXOffset;
    private static int optionsScreenBtnYOffset;

    private Config() {
    }

    public static void init(OverlayerData data) {
        titleScreenBtnXOffset = data.getTitleScreenBtnXOffset();
        titleScreenBtnYOffset = data.getTitleScreenBtnYOffset();
        optionsScreenBtnXOffset = data.getOptionsScreenBtnXOffset();
        optionsScreenBtnYOffset = data.getOptionsScreenBtnYOffset();
    }

    public static OverlayerData writeData(OverlayerData data) {
        data.setTitleScreenBtnXOffset(titleScreenBtnXOffset);
        data.setTitleScreenBtnYOffset(titleScreenBtnYOffset);
        data.setOptionsScreenBtnXOffset(optionsScreenBtnXOffset);
        data.setOptionsScreenBtnYOffset(optionsScreenBtnYOffset);
        return data;
    }

    public static int getOptionsScreenBtnYOffset() {
        return optionsScreenBtnYOffset;
    }

    public static void setOptionsScreenBtnYOffset(int optionsScreenBtnYOffset) {
        Config.optionsScreenBtnYOffset = optionsScreenBtnYOffset;
    }

    public static int getOptionsScreenBtnXOffset() {
        return optionsScreenBtnXOffset;
    }

    public static void setOptionsScreenBtnXOffset(int optionsScreenBtnXOffset) {
        Config.optionsScreenBtnXOffset = optionsScreenBtnXOffset;
    }

    public static int getTitleScreenBtnYOffset() {
        return titleScreenBtnYOffset;
    }

    public static void setTitleScreenBtnYOffset(int titleScreenBtnYOffset) {
        Config.titleScreenBtnYOffset = titleScreenBtnYOffset;
    }

    public static int getTitleScreenBtnXOffset() {
        return titleScreenBtnXOffset;
    }

    public static void setTitleScreenBtnXOffset(int titleScreenBtnXOffset) {
        Config.titleScreenBtnXOffset = titleScreenBtnXOffset;
    }

    public static boolean getIsOpenContainerScreen() {
        return isOpenContainerScreen;
    }

    public static void setIsOpenContainerScreen(boolean openContainerScreen) {
        isOpenContainerScreen = openContainerScreen;
    }
}
