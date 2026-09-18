package com.skrepy.overlayer.client.gui;


import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;

public class OverlayerToast implements Toast {

    private final Component title;
    private final Component message;
    private final Type type;
    private final long duration = 5000L;
    private long displayStartTime = -1L;

    private OverlayerToast(Component title, Component message, Type type) {
        this.title = title;
        this.message = message;
        this.type = type;
    }

    public static void showInfo(Component title, Component message) {
        show(title, message, Type.INFO);
    }

    public static void showWarning(Component title, Component message) {
        show(title, message, Type.WARNING);
    }

    public static void showError(Component title, Component message) {
        show(title, message, Type.ERROR);
    }

    private static void show(Component title, Component message, Type type) {
        OverlayerToast toast = new OverlayerToast(title, message, type);
        Minecraft.getInstance().gui.toastManager().addToast(toast);
        //Overlayer.LOGGER.debug("Toast added: title={}, message={}, type={}", title.getString(), message != null ? message.getString() : "null", type);
    }

    @Override
    public void update(@NotNull ToastManager toastManager, long time) {
        if (displayStartTime == -1) {
            displayStartTime = System.currentTimeMillis();
            //Overlayer.LOGGER.debug("Toast update: start time set to {}", displayStartTime);
        }
    }

    @Override
    public @NotNull Visibility getWantedVisibility() {
        if (displayStartTime == -1) {
            return Visibility.SHOW;
        }
        long elapsed = System.currentTimeMillis() - displayStartTime;
        return elapsed < duration ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, Font font, long time) {
        //Overlayer.LOGGER.debug("Toast extractRenderState called at time={}", time);
        int width = this.width();
        int height = this.height();
        int x = 0, y = 0;

        // 阴影
        guiGraphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x44000000);
        // 主背景
        guiGraphics.fill(x, y, x + width, y + height, 0x01001F);
        // 左侧彩色边框
        guiGraphics.fill(x, y, x + 5, y + height, type.borderColor);

        // 图标圆形背景
        int iconSize = 16;
        int iconX = x + 12;
        int iconY = y + (height - iconSize) / 2;
        guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0x020033);
        guiGraphics.fill(iconX + 1, iconY + 1, iconX + iconSize - 1, iconY + iconSize - 1, 0x01001F);

        // 图标字符
        String iconStr = type.icon;
        int iconCharWidth = font.width(iconStr);
        int iconCharHeight = font.lineHeight;
        int iconCharX = iconX + (iconSize - iconCharWidth) / 2;
        int iconCharY = iconY + (iconSize - iconCharHeight) / 2;
        guiGraphics.text(font, iconStr, iconCharX, iconCharY, type.borderColor, false);
        //Overlayer.LOGGER.debug("Toast icon drawn at ({},{}) color={}", iconCharX, iconCharY, Integer.toHexString(type.borderColor));

        // 文字
        int textX = iconX + iconSize + 10;
        int textY = y + (height - (message != null ? 30 : 20)) / 2;
        guiGraphics.text(font, title, textX, textY, type.textColor, false);
        //Overlayer.LOGGER.debug("Toast title drawn at ({},{}) color={}", textX, textY, Integer.toHexString(type.textColor));
        if (message != null) {
            guiGraphics.text(font, message, textX, textY + 12, 0xFF888888, false);
            //Overlayer.LOGGER.debug("Toast message drawn at ({},{}) color=FF888888", textX, textY + 12);
        }
    }

    @Override
    public int width() {
        Font font = Minecraft.getInstance().font;
        int titleWidth = font.width(title);
        int msgWidth = message != null ? font.width(message) : 0;
        return Math.max(220, Math.max(titleWidth, msgWidth) + 46 + 20);
    }

    @Override
    public int height() {
        return message != null ? 44 : 32;
    }

    public enum Type {
        INFO(0xFF2A7FFF, 0xFFFFFFFF, 0x01001F, "ℹ"), WARNING(0xFFFF8C00, 0xFFFFFFFF, 0x01001F, "⚠"), ERROR(0xFFFF1744, 0xFFFFFFFF, 0x01001F, "✕");

        final int borderColor;
        final int textColor;
        final int bgColor;
        final String icon;

        Type(int borderColor, int textColor, int bgColor, String icon) {
            this.borderColor = borderColor;
            this.textColor = textColor;
            this.bgColor = bgColor;
            this.icon = icon;
        }
    }
}
