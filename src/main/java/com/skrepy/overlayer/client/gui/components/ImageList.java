package com.skrepy.overlayer.client.gui.components;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skrepy.overlayer.data.ImageEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ImageList extends ObjectSelectionList<ImageList.Entry> {
    private static final Component EMPTY_TEXT = Component.literal("请添加图片实例");
    private static final int DELETE_BUTTON_WIDTH = 20;
    private static final int DELETE_BUTTON_PADDING = 4;

    private final Font font;
    private final Consumer<ImageEntry> onRemove;
    private final Consumer<ImageEntry> onEdit;

    public ImageList(Minecraft minecraft, int width, int height, int top, int itemHeight, Font font, Consumer<ImageEntry> onRemove, Consumer<ImageEntry> onEdit) {
        super(minecraft, width, height, top, itemHeight);
        this.font = font;
        this.onRemove = onRemove;
        this.onEdit = onEdit;
    }

    public void updateEntries(List<ImageEntry> entries) {
        this.clearEntries();
        for (int i = 0; i < entries.size(); i++) {
            this.addEntry(new Entry(entries.get(i), i + 1));
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width - 8;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        RenderSystem.disableBlend();
        if (this.getItemCount() == 0) {
            int x = this.getX() + this.width / 2 - font.width(EMPTY_TEXT) / 2;
            int y = this.getY() + this.height / 2 - 5;
            guiGraphics.drawString(font, EMPTY_TEXT, x, y, 0x888888);
        }
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> {
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
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Font font = ImageList.this.font;

            // 编号
            guiGraphics.drawString(font, numberText, left + 4, top + (height - 8) / 2, 0xFFFFFF);

            // 缩略图
            int thumbX = left + 30;
            int thumbY = top + 2;
            int thumbSize = height - 4;
            ResourceLocation tex = imageEntry.getThumbnail(Minecraft.getInstance().getTextureManager(), null);
            if (tex != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                guiGraphics.blit(tex, thumbX, thumbY, 0, 0, thumbSize, thumbSize, thumbSize, thumbSize);
                RenderSystem.disableBlend();
            } else {
                guiGraphics.fill(thumbX, thumbY, thumbX + thumbSize, thumbY + thumbSize, 0xFF888888);
                guiGraphics.drawString(font, "?", thumbX + thumbSize / 2 - 4, thumbY + thumbSize / 2 - 4, 0xFFFFFF);
            }

            // 路径
            String pathStr = imageEntry.getAbsolutePath().toString();
            int maxPathWidth = width - 30 - thumbSize - 10 - DELETE_BUTTON_WIDTH - DELETE_BUTTON_PADDING * 2 - 4;
            String display = font.plainSubstrByWidth(pathStr, maxPathWidth);
            guiGraphics.drawString(font, display, left + 30 + thumbSize + 6, top + (height - 8) / 2, 0xAAAAAA);

            // 删除按钮
            int deleteX = left + width - DELETE_BUTTON_WIDTH - DELETE_BUTTON_PADDING;
            int deleteY = top + (height - 16) / 2;
            int deleteW = DELETE_BUTTON_WIDTH;
            int deleteH = 16;
            boolean hovered = mouseX >= deleteX && mouseX <= deleteX + deleteW && mouseY >= deleteY && mouseY <= deleteY + deleteH;
            guiGraphics.fill(deleteX, deleteY, deleteX + deleteW, deleteY + deleteH, hovered ? 0xCC555555 : 0x44FFFFFF);
            guiGraphics.drawString(font, "×", deleteX + (deleteW - font.width("×")) / 2, deleteY + (deleteH - 8) / 2, 0xFF6666);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // 检测删除按钮
            int rowLeft = ImageList.this.getRowLeft();
            int rowWidth = ImageList.this.getRowWidth();
            int deleteX = rowLeft + rowWidth - DELETE_BUTTON_WIDTH - DELETE_BUTTON_PADDING;
            int rowTop = ImageList.this.getRowTop(ImageList.this.children().indexOf(this));
            int deleteY = rowTop + (ImageList.this.itemHeight - 16) / 2;
            int deleteH = 16;

            if (mouseX >= deleteX && mouseX <= deleteX + DELETE_BUTTON_WIDTH && mouseY >= deleteY && mouseY <= deleteY + deleteH) {
                onRemove.accept(imageEntry);
                return true;
            }

            // 否则为编辑
            onEdit.accept(imageEntry);
            return true;
        }
    }
}
