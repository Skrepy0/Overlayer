package com.skrepy.overlayer.client.gui.components;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.skrepy.overlayer.data.ImageEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ImageList extends ObjectSelectionList<ImageList.@NotNull Entry> {
    private static final int DELETE_BUTTON_WIDTH = 20;
    private static final int DELETE_BUTTON_PADDING = 4;

    private final Font font;
    private final Consumer<ImageEntry> onRemove;
    private final Consumer<ImageEntry> onEdit;
    private final int itemHeight;

    public ImageList(Minecraft minecraft, int width, int height, int top, int itemHeight, Font font, Consumer<ImageEntry> onRemove, Consumer<ImageEntry> onEdit) {
        super(minecraft, width, height, top, itemHeight);
        this.font = font;
        this.onRemove = onRemove;
        this.onEdit = onEdit;
        this.itemHeight = itemHeight;
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public void updateEntries(List<ImageEntry> entries) {
        this.clearEntries();
        for (int i = 0; i < entries.size(); i++) {
            this.addEntry(new Entry(entries.get(i), i + 1));
        }
        this.refresh();
    }

    public void refresh() {
        this.setScrollAmount(0);
        if (this.getItemCount() > 0) {
            this.getRowTop(0);
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int scrollBarX() {
        return this.getX() + this.width - 8;
    }

    public class Entry extends ObjectSelectionList.Entry<@NotNull Entry> {
        private final ImageEntry imageEntry;
        private final int number;
        private final Component numberText;

        public Entry(ImageEntry entry, int number) {
            this.imageEntry = entry;
            this.number = number;
            this.numberText = Component.literal(String.valueOf(number));
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal("图片条目 " + number);
        }

        @Override
        public void extractContent(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            int idx = ImageList.this.children().indexOf(this);
            if (idx < 0 || idx >= ImageList.this.getItemCount()) {
                return;
            }

            int y = ImageList.this.getY() + 4 + idx * ImageList.this.itemHeight - (int) ImageList.this.scrollAmount();
            int left = ImageList.this.getRowLeft();
            int width = ImageList.this.getRowWidth();
            int height = ImageList.this.itemHeight;

            // ---- 编号 ----
            guiGraphics.text(font, numberText, left + 4, y + (height - 8) / 2, 0xFFFFFFFF, true);

            // ---- 缩略图 ----
            int thumbX = left + 30;
            int thumbY = y + 2;
            int thumbSize = height - 4;
            Identifier tex = imageEntry.getThumbnail(Minecraft.getInstance().getTextureManager());
            if (tex != null) {
                guiGraphics.blit(
                        RenderPipelines.GUI_TEXTURED, tex, thumbX, thumbY, 0.0f, 0.0f, thumbSize, thumbSize, thumbSize, thumbSize, thumbSize, thumbSize, -1
                );
            } else {
                guiGraphics.fill(thumbX, thumbY, thumbX + thumbSize, thumbY + thumbSize, 0xFF888888);
                guiGraphics.text(font, Component.literal("?"), thumbX + thumbSize / 2 - 4, thumbY + thumbSize / 2 - 4, 0xFFFFFFFF, true);
            }

            // ---- 路径 ----
            String pathStr = imageEntry.getAbsolutePath().toString();
            int maxPathWidth = width - 30 - thumbSize - 10 - DELETE_BUTTON_WIDTH - DELETE_BUTTON_PADDING * 2 - 4;
            if (maxPathWidth < 20) maxPathWidth = 20;
            String display = font.plainSubstrByWidth(pathStr, maxPathWidth);
            if (font.width(display) < font.width(pathStr) && maxPathWidth > 20) {
                display = display + "...";
            }
            guiGraphics.text(font, display, left + 30 + thumbSize + 6, y + (height - 8) / 2, 0xFFAAAAAA, true);

            // ---- 删除按钮 ----
            int deleteX = left + width - DELETE_BUTTON_WIDTH - DELETE_BUTTON_PADDING;
            int deleteY = y + (height - 16) / 2;
            int deleteW = DELETE_BUTTON_WIDTH;
            int deleteH = 16;
            boolean hovered = mouseX >= deleteX && mouseX <= deleteX + deleteW && mouseY >= deleteY && mouseY <= deleteY + deleteH;
            guiGraphics.fill(deleteX, deleteY, deleteX + deleteW, deleteY + deleteH, hovered ? 0xCC555555 : 0x44FFFFFF);
            guiGraphics.text(font, "×", deleteX + (deleteW - font.width("×")) / 2, deleteY + (deleteH - 8) / 2, 0xFFFF6666, true);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            double mouseX = event.x();
            double mouseY = event.y();

            int rowLeft = ImageList.this.getRowLeft();
            int rowWidth = ImageList.this.getRowWidth();
            int deleteX = rowLeft + rowWidth - DELETE_BUTTON_WIDTH - DELETE_BUTTON_PADDING;
            int idx = ImageList.this.children().indexOf(this);
            int rowTop = ImageList.this.getY() + 4 + idx * ImageList.this.itemHeight - (int) ImageList.this.scrollAmount();
            int deleteY = rowTop + (ImageList.this.itemHeight - 16) / 2;

            if (mouseX >= deleteX && mouseX <= deleteX + DELETE_BUTTON_WIDTH && mouseY >= deleteY && mouseY <= deleteY + 16) {
                onRemove.accept(imageEntry);
                return true;
            }

            onEdit.accept(imageEntry);
            return true;
        }
    }
}
