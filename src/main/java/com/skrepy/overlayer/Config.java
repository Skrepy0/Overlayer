package com.skrepy.overlayer;

public class Config {
    private static boolean isOpenContainerScreen;

    public static boolean getIsOpenContainerScreen() {
        return isOpenContainerScreen;
    }

    public static void setIsOpenContainerScreen(boolean openContainerScreen) {
        isOpenContainerScreen = openContainerScreen;
    }
}
