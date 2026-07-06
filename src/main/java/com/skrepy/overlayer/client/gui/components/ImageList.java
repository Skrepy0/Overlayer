package com.skrepy.overlayer.client.gui.components;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.skrepy.overlayer.data.ImageEntry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ImageList extends EntryListWidget<ImageList.Entry> {
    private static final Text EMPTY_TEXT = Text.literal("请添加图片实例");
    private static final int DELETE_BUTTON_WIDTH = 20;
    private static final int DELETE_BUTTON_PADDING = 4;

    private final TextRenderer textRenderer;
    private final Consumer<ImageEntry> onRemove;
    private final Consumer<ImageEntry> onEdit;

    public ImageList(MinecraftClient client, int width, int height, int top, int itemHeight, TextRenderer textRenderer, Consumer<ImageEntry> onRemove, Consumer<ImageEntry> onEdit) {
        super(client, width, height, top, itemHeight);
        this.textRenderer = textRenderer;
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
    protected int getScrollbarX() {
        return this.getX() + this.width - 8;
    }

    @Override
    public void renderWidget(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
        if (this.getEntryCount() == 0) {
            context.drawCenteredTextWithShadow(
                    textRenderer, EMPTY_TEXT, this.getX() + this.width / 2, this.getY() + this.height / 2 - 5, 0x888888
            );
        }
    }

    // ========== 实现抽象方法 ==========
    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        // 添加列表本身的叙述
        builder.put(NarrationPart.TITLE, Text.translatable("narrator.screen.list"));
        // 如果有选中条目，可以添加其叙述
        if (this.getSelectedOrNull() != null) {
            builder.put(NarrationPart.HINT, Text.literal("选中条目: " + this.getSelectedOrNull().getNarration().getString()));
        }
    }

    // ---------- 内部类 Entry ----------
    public class Entry extends EntryListWidget.Entry<Entry> {
        private final ImageEntry imageEntry;
        private final int number;
        private final Text numberText;

        public Entry(ImageEntry entry, int number) {
            this.imageEntry = entry;
            this.number = number;
            this.numberText = Text.literal(String.valueOf(number));
        }

        // 注意：此方法不是重写，是自定义方法，不要加 @Override
        public @NotNull Text getNarration() {
            return Text.literal("图片条目 " + number);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
            TextRenderer font = ImageList.this.textRenderer;

            context.drawText(font, numberText, x + 4, y + (entryHeight - 8) / 2, 0xFFFFFF, false);

            int thumbX = x + 30;
            int thumbY = y + 2;
            int thumbSize = entryHeight - 4;
            Identifier tex = imageEntry.getThumbnail(MinecraftClient.getInstance().getTextureManager());
            if (tex != null) {
                context.drawTexture(tex, thumbX, thumbY, 0, 0, thumbSize, thumbSize, thumbSize, thumbSize);
            } else {
                context.fill(thumbX, thumbY, thumbX + thumbSize, thumbY + thumbSize, 0xFF888888);
                context.drawText(font, "?", thumbX + thumbSize / 2 - 4, thumbY + thumbSize / 2 - 4, 0xFFFFFF, false);
            }

            String pathStr = imageEntry.getPath();
            int maxPathWidth = entryWidth - 30 - thumbSize - 10 - DELETE_BUTTON_WIDTH - DELETE_BUTTON_PADDING * 2 - 4;
            String display = font.trimToWidth(pathStr, maxPathWidth);
            context.drawText(font, display, x + 30 + thumbSize + 6, y + (entryHeight - 8) / 2, 0xAAAAAA, false);

            int deleteX = x + entryWidth - DELETE_BUTTON_WIDTH - DELETE_BUTTON_PADDING;
            int deleteY = y + (entryHeight - 16) / 2;
            int deleteW = DELETE_BUTTON_WIDTH;
            int deleteH = 16;
            boolean hoveredDelete = mouseX >= deleteX && mouseX <= deleteX + deleteW && mouseY >= deleteY && mouseY <= deleteY + deleteH;
            int deleteBg = hoveredDelete ? 0xCCFF4444 : 0x44FFFFFF;
            context.fill(deleteX, deleteY, deleteX + deleteW, deleteY + deleteH, deleteBg);
            context.drawText(font, "×", deleteX + (deleteW - font.getWidth("×")) / 2, deleteY + (deleteH - 8) / 2, 0xFF6666, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int rowLeft = ImageList.this.getRowLeft();
            int rowWidth = ImageList.this.getRowWidth();
            int deleteX = rowLeft + rowWidth - DELETE_BUTTON_WIDTH - DELETE_BUTTON_PADDING;
            int rowTop = ImageList.this.getRowTop(ImageList.this.children().indexOf(this));
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
