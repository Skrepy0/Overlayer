package com.skrepy.overlayer.client.gui;

import static com.skrepy.overlayer.Overlayer.validFormat;
import static com.skrepy.overlayer.manager.OverlayerManager.*;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.client.gui.components.ImageList;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class OverlayerSettingsScreen extends Screen {
    private static final Component TITLE = Component.translatable("overlayer.screen.settings_page.title");
    private static final Component DONE = CommonComponents.GUI_DONE;
    private static final Component ADD = Component.translatable("overlayer.screen.settings_page.button.add");
    private static final Component CLEAR = Component.translatable("overlayer.screen.settings_page.button.clear");
    private static final int LIST_ENTRY_HEIGHT = 40;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen lastScreen;
    private final OverlayerManager manager;
    private final List<ImageEntry> imageEntries;

    private EditBox pathInput;
    private Button browseButton;
    private Button doneButton;
    private Button addButton;
    private Button clearButton;
    private ImageList list;
    private StringWidget titleWidget;

    private int titleY = 20;
    private int inputY = 50;

    public OverlayerSettingsScreen(Screen lastScreen) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.manager = OverlayerManager.getInstance();
        this.imageEntries = manager.getInstances();
    }

    @Override
    protected void init() {
        super.init();

        titleY = 20;
        this.titleWidget = new StringWidget(TITLE, this.font);
        this.titleWidget.setX(this.width / 2 - this.font.width(TITLE) / 2);
        this.titleWidget.setY(titleY);
        this.addRenderableWidget(this.titleWidget);

        inputY = titleY + 30;
        this.pathInput = new EditBox(this.font, this.width / 2 - 110, inputY, 200, 20, Component.literal("Image path"));
        this.pathInput.setMaxLength(Integer.MAX_VALUE);
        this.addRenderableWidget(this.pathInput);

        this.browseButton = Button.builder(Component.literal("..."), (btn) -> this.openFileChooser()).pos(this.width / 2 + 95, inputY).size(20, 20).tooltip(Tooltip.create(Component.translatable("overlayer.screen.button.select_file.tooltip"))).build();
        this.addRenderableWidget(this.browseButton);

        int buttonRowY = this.height - 30;
        int totalWidth = BUTTON_WIDTH * 3 + 8;
        int startX = this.width / 2 - totalWidth / 2;

        this.doneButton = Button.builder(DONE, (btn) -> this.onClose()).pos(startX, buttonRowY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.doneButton);

        this.addButton = Button.builder(ADD, (btn) -> this.addCurrentPath()).pos(startX + BUTTON_WIDTH + 4, buttonRowY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.addButton);

        this.clearButton = Button.builder(CLEAR, (btn) -> this.clearList()).pos(startX + (BUTTON_WIDTH + 4) * 2, buttonRowY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.clearButton);

        this.list = new ImageList(this.minecraft, 0, 0, 0, LIST_ENTRY_HEIGHT, this.font, this::removeEntry, this::openEditScreen);
        this.list.updateEntries(this.imageEntries);
        this.addRenderableWidget(this.list);

        this.repositionElements();
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        if (list != null && list.isEmpty()) {
            Component emptyText = Component.literal("请添加图片实例");
            int x = list.getX() + list.getWidth() / 2 - font.width(emptyText) / 2;
            int y = list.getY() + list.getHeight() / 2 - 5;
            guiGraphics.text(font, emptyText, x, y, 0x888888, true);
        }
    }

    private void openEditScreen(ImageEntry entry) {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new ImageEditScreen(this, entry, (edited) -> {
                this.list.updateEntries(this.imageEntries);
                manager.save();
            }));
        }
    }

    @Override
    protected void repositionElements() {
        this.titleWidget.setX(this.width / 2 - this.font.width(TITLE) / 2);
        this.titleWidget.setY(titleY);

        int inputY = titleY + 30;
        int centerX = this.width / 2;
        this.pathInput.setX(centerX - 110);
        this.pathInput.setY(inputY);
        this.browseButton.setX(centerX + 95);
        this.browseButton.setY(inputY);

        int buttonRowY = this.height - 30;
        int totalWidth = BUTTON_WIDTH * 3 + 8;
        int startX = this.width / 2 - totalWidth / 2;
        this.doneButton.setX(startX);
        this.doneButton.setY(buttonRowY);
        this.addButton.setX(startX + BUTTON_WIDTH + 4);
        this.addButton.setY(buttonRowY);
        this.clearButton.setX(startX + (BUTTON_WIDTH + 4) * 2);
        this.clearButton.setY(buttonRowY);

        int listTop = inputY + 20 + 10;
        int listBottom = buttonRowY - 10;
        int listWidth = this.width - 20;
        int listHeight = listBottom - listTop;
        if (listHeight < 0) listHeight = 0;

        this.list.setSize(listWidth, listHeight);
        this.list.setY(listTop);
        this.list.setX(10);
        this.list.refresh();

        Overlayer.LOGGER.debug("List reposition: size={}x{}, y={}, x={}", listWidth, listHeight, listTop, 10);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.lastScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private void openFileChooser() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String result = selectFile(stack);
            if (result != null) {
                this.pathInput.setValue(result);
            }
        }
    }

    private void addCurrentPath() {
        String path = this.pathInput.getValue().trim();
        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        }
        if (path.isEmpty()) {
            OverlayerToast.showWarning(
                    Component.translatable("overlayer.toast.warning.invalid_path.title"), Component.translatable("overlayer.toast.warning.invalid_path.meg.empty_path")
            );
            return;
        }
        String extension = getFileExtension(path);
        if (extension.isEmpty()) {
            OverlayerToast.showWarning(
                    Component.translatable("overlayer.toast.warning.invalid_path.title"), Component.translatable("overlayer.toast.warning.invalid_path.meg.no_extension", extension)
            );
            return;
        }
        if (!validFormat.contains(extension)) {
            OverlayerToast.showWarning(
                    Component.translatable("overlayer.toast.warning.unsupported_format.title"), Component.translatable("overlayer.toast.warning.unsupported_format.meg", extension)
            );
            return;
        }
        if (!fileExists(path)) {
            OverlayerToast.showWarning(
                    Component.translatable("overlayer.toast.warning.invalid_path.title"), Component.translatable("overlayer.toast.warning.invalid_path.meg.no_file", extension)
            );
            return;
        }
        int maxId = imageEntries.stream().mapToInt(ImageEntry::getId).max().orElse(0);
        int newId = maxId + 1;
        ImageEntry newEntry = new ImageEntry(newId, path);
        imageEntries.add(newEntry);
        this.list.updateEntries(imageEntries);
        this.pathInput.setValue("");
        manager.save();
    }

    private void removeEntry(ImageEntry entry) {
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            imageEntries.remove(entry);
                            this.list.updateEntries(imageEntries);
                            manager.save();
                        }
                        this.minecraft.setScreenAndShow(this);
                    }, Component.translatable("overlayer.screen.delete_confirm.title"), Component.translatable("overlayer.screen.delete_confirm.meg"), Component.translatable("overlayer.screen.common.delete"), CommonComponents.GUI_CANCEL
            ));
        }
    }

    private void clearList() {
        if (imageEntries.isEmpty()) {
            return;
        }
        if (this.minecraft != null) {
            this.minecraft.setScreenAndShow(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            imageEntries.clear();
                            this.list.updateEntries(imageEntries);
                            manager.save();
                        }
                        this.minecraft.setScreenAndShow(this);
                    }, Component.translatable("overlayer.screen.delete_all_confirm.title"), Component.translatable("overlayer.screen.delete_all_confirm.meg"), Component.translatable("overlayer.screen.common.delete_all"), CommonComponents.GUI_CANCEL
            ));
        }
    }
}
