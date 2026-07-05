package com.skrepy.overlayer;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    public static boolean isIsOpenContainerScreen() {
        return isOpenContainerScreen;
    }

    public static void setIsOpenContainerScreen(boolean isOpenContainerScreen) {
        Config.isOpenContainerScreen = isOpenContainerScreen;
    }

    private static boolean isOpenContainerScreen;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue titleScreenBtnXOffset = BUILDER.defineInRange("titleScreenBtnXOffset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue titleScreenBtnYOffset = BUILDER.defineInRange("titleScreenBtnYOffset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue optionsScreenBtnXOffset = BUILDER.defineInRange("optionsScreenBtnXOffset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue optionsScreenBtnYOffset = BUILDER.defineInRange("optionsScreenBtnYOffset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static int getTitleScreenBtnXOffset() {
        return titleScreenBtnXOffset.get();
    }

    public static void setTitleScreenBtnXOffset(int titleScreenBtnXOffset) {
        Config.titleScreenBtnXOffset.set(titleScreenBtnXOffset);
    }

    public static int getTitleScreenBtnYOffset() {
        return titleScreenBtnYOffset.get();
    }

    public static void setTitleScreenBtnYOffset(int titleScreenBtnYOffset) {
        Config.titleScreenBtnYOffset.set(titleScreenBtnYOffset);
    }

    public static int getOptionsScreenBtnXOffset() {
        return optionsScreenBtnXOffset.get();
    }

    public static void setOptionsScreenBtnXOffset(int optionsScreenBtnXOffset) {
        Config.optionsScreenBtnXOffset.set(optionsScreenBtnXOffset);
    }

    public static int getOptionsScreenBtnYOffset() {
        return optionsScreenBtnYOffset.get();
    }

    public static void setOptionsScreenBtnYOffset(int optionsScreenBtnYOffset) {
        Config.optionsScreenBtnYOffset.set(optionsScreenBtnYOffset);
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
