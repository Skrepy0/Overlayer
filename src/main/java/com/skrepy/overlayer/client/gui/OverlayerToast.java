package com.skrepy.overlayer.client.gui;

import org.jetbrains.annotations.NotNull;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class OverlayerToast implements Toast {
    private final Text title;
    private final Text message;
    private final Type type;
    private final long displayTime = 5000L;
    private long firstRenderTime = -1;
    private Visibility currentVisibility = Visibility.SHOW;

    private OverlayerToast(Text title, Text message, Type type) {
        this.title = title;
        this.message = message;
        this.type = type;
    }

    // ---------- 工厂方法 ----------
    public static void showInfo(Text title, Text message) {
        show(title, message, Type.INFO);
    }

    public static void showWarning(Text title, Text message) {
        show(title, message, Type.WARNING);
    }

    public static void showError(Text title, Text message) {
        show(title, message, Type.ERROR);
    }

    private static void show(Text title, Text message, Type type) {
        OverlayerToast toast = new OverlayerToast(title, message, type);
        MinecraftClient.getInstance().getToastManager().add(toast);
        System.out.println("[OverlayerToast] Added: " + title.getString());
    }

    @Override
    public void update(ToastManager manager, long time) {
        if (firstRenderTime == -1) {
            firstRenderTime = time;
        }
        currentVisibility = (time - firstRenderTime > displayTime) ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public Visibility getVisibility() {
        return currentVisibility;
    }

    @Override
    public void draw(@NotNull DrawContext context, @NotNull TextRenderer textRenderer, long startTime) {
        int width = this.getWidth();
        int height = this.getHeight();
        int x = 0, y = 0;

        // 阴影
        context.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x44000000);
        // 主背景
        context.fill(x, y, x + width, y + height, type.bgColor);

        // 左侧彩色边框
        context.fill(x, y, x + 5, y + height, type.borderColor);

        // 图标
        int iconSize = 16;
        int iconX = x + 12;
        int iconY = y + (height - iconSize) / 2;
        context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0x020033);
        context.fill(iconX + 1, iconY + 1, iconX + iconSize - 1, iconY + iconSize - 1, 0xFF01001F);

        String iconStr = type.icon;
        int iconCharWidth = textRenderer.getWidth(iconStr);
        int iconCharHeight = textRenderer.fontHeight;
        int iconCharX = iconX + (iconSize - iconCharWidth) / 2;
        int iconCharY = iconY + (iconSize - iconCharHeight) / 2;
        context.drawText(textRenderer, iconStr, iconCharX, iconCharY, type.borderColor, false);

        // 文字
        int textX = iconX + iconSize + 10;
        int textY = y + (height - (message != null ? 30 : 20)) / 2;
        context.drawText(textRenderer, title, textX, textY, type.textColor, false);
        if (message != null) {
            context.drawText(textRenderer, message, textX, textY + 12, 0xFF888888, false);
        }

    }

    @Override
    public int getWidth() {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int titleWidth = tr.getWidth(title);
        int msgWidth = message != null ? tr.getWidth(message) : 0;
        return Math.max(220, Math.max(titleWidth, msgWidth) + 46 + 20);
    }

    @Override
    public int getHeight() {
        return message != null ? 44 : 32;
    }

    @Override
    public Object getType() {
        return this;
    }

    public enum Type {
        INFO(0xFF2A7FFF, 0xFFFFFFFF, 0xFF01001F, "ℹ"), WARNING(0xFFFF8C00, 0xFFFFFFFF, 0xFF01001F, "⚠"), ERROR(0xFFFF1744, 0xFFFFFFFF, 0xFF01001F, "✕");

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
