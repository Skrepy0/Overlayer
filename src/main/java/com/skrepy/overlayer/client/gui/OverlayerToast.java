package com.skrepy.overlayer.client.gui;

import org.jetbrains.annotations.NotNull;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
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
        // 调试日志，确认添加
        System.out.println("[OverlayerToast] Added: " + title.getString());
    }

    @Override
    public @NotNull Visibility draw(@NotNull DrawContext context, @NotNull ToastManager manager, long startTime) {
        // 添加日志，确认被调用
        System.out.println("[OverlayerToast] draw called!");

        if (firstRenderTime == -1) {
            firstRenderTime = startTime;
        }

        if (startTime - firstRenderTime > displayTime) {
            return Visibility.HIDE;
        }

        int width = this.getWidth();
        int height = this.getHeight();
        int x = 0, y = 0;

        // 阴影
        context.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x44000000);
        // 主背景（不透明）
        context.fill(x, y, x + width, y + height, type.bgColor); // 使用枚举中的背景色

        // 左侧彩色边框
        context.fill(x, y, x + 5, y + height, type.borderColor);

        // 图标
        int iconSize = 16;
        int iconX = x + 12;
        int iconY = y + (height - iconSize) / 2;
        context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0x020033);
        context.fill(iconX + 1, iconY + 1, iconX + iconSize - 1, iconY + iconSize - 1, 0xFF01001F);

        String iconStr = type.icon;
        int iconCharWidth = MinecraftClient.getInstance().textRenderer.getWidth(iconStr);
        int iconCharHeight = MinecraftClient.getInstance().textRenderer.fontHeight;
        int iconCharX = iconX + (iconSize - iconCharWidth) / 2;
        int iconCharY = iconY + (iconSize - iconCharHeight) / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, iconStr, iconCharX, iconCharY, type.borderColor, false);

        // 文字
        int textX = iconX + iconSize + 10;
        int textY = y + (height - (message != null ? 30 : 20)) / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, title, textX, textY, type.textColor, false);
        if (message != null) {
            context.drawText(MinecraftClient.getInstance().textRenderer, message, textX, textY + 12, 0xFF888888, false);
        }

        return Visibility.SHOW;
    }

    @Override
    public int getWidth() {
        int titleWidth = MinecraftClient.getInstance().textRenderer.getWidth(title);
        int msgWidth = message != null ? MinecraftClient.getInstance().textRenderer.getWidth(message) : 0;
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
