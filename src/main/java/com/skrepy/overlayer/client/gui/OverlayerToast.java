package com.skrepy.overlayer.client.gui;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OverlayerToast implements Toast {
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

    private final Component title;
    private final Component message;
    private final Type type;
    private final long displayTime;
    private long firstRenderTime = -1;

    private OverlayerToast(Component title, Component message, Type type) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.displayTime = 5000L;
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
        Minecraft.getInstance().getToasts().addToast(new OverlayerToast(title, message, type));
    }

    @Override
    public @NotNull Visibility render(@NotNull GuiGraphics guiGraphics, @NotNull ToastComponent toastComponent, long timeSinceLastVisible) {
        if (firstRenderTime == -1) {
            firstRenderTime = timeSinceLastVisible;
        }

        if (timeSinceLastVisible - firstRenderTime > displayTime) {
            return Visibility.HIDE;
        }

        int width = this.width();
        int height = this.height();
        int x = 0, y = 0;

        // ---- 绘制背景（带半透明阴影效果） ----
        // 1. 阴影（向右下方偏移2像素，透明度更高）
        guiGraphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x44000000);

        // 2. 主背景（略微圆角感：使用纯色，但用左右填充营造圆角视觉）
        guiGraphics.fill(x, y, x + width, y + height, 0x01001F);

        // ---- 左侧彩色边框 ----
        guiGraphics.fill(x, y, x + 5, y + height, type.borderColor);

        // ---- 绘制圆形图标背景 ----
        int iconSize = 16;
        int iconX = x + 12;
        int iconY = y + (height - iconSize) / 2;

        // 绘制白色或浅色圆形底色
        guiGraphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0x020033);
        guiGraphics.fill(iconX + 1, iconY + 1, iconX + iconSize - 1, iconY + iconSize - 1, 0x01001F);

        // ---- 绘制图标字符 ----
        String iconStr = type.icon;
        int iconCharWidth = Minecraft.getInstance().font.width(iconStr);
        int iconCharHeight = Minecraft.getInstance().font.lineHeight;
        int iconCharX = iconX + (iconSize - iconCharWidth) / 2;
        int iconCharY = iconY + (iconSize - iconCharHeight) / 2;
        guiGraphics.drawString(toastComponent.getMinecraft().font, iconStr, iconCharX, iconCharY, type.borderColor, false);

        // ---- 绘制文字 ----
        int textX = iconX + iconSize + 10;
        int textY = y + (height - (message != null ? 30 : 20)) / 2;

        // 标题
        guiGraphics.drawString(toastComponent.getMinecraft().font, title, textX, textY, type.textColor, false);

        // 内容
        if (message != null) {
            guiGraphics.drawString(toastComponent.getMinecraft().font, message, textX, textY + 12, 0xFF888888, false);
        }

        return Visibility.SHOW;
    }

    @Override
    public int width() {
        int titleWidth = Minecraft.getInstance().font.width(title);
        int msgWidth = message != null ? Minecraft.getInstance().font.width(message) : 0;
        return Math.max(220, Math.max(titleWidth, msgWidth) + 46 + 20);
    }

    @Override
    public int height() {
        return message != null ? 44 : 32;
    }
}
