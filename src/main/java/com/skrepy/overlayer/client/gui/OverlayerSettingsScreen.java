package com.skrepy.overlayer.client.gui;

import static com.skrepy.overlayer.Overlayer.validFormat;
import static com.skrepy.overlayer.manager.OverlayerManager.*;

import java.util.List;

import org.lwjgl.system.MemoryStack;

import com.skrepy.overlayer.client.gui.components.ImageList;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class OverlayerSettingsScreen extends Screen {
    private static final Text TITLE = Text.translatable("overlayer.screen.settings_page.title");
    private static final Text DONE = ScreenTexts.DONE;
    private static final Text ADD = Text.translatable("overlayer.screen.settings_page.button.add");
    private static final Text CLEAR = Text.translatable("overlayer.screen.settings_page.button.clear");
    private static final int LIST_ENTRY_HEIGHT = 40;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen lastScreen;
    private final OverlayerManager manager;
    private final List<ImageEntry> imageEntries;

    private TextFieldWidget pathInput;
    private ButtonWidget browseButton;
    private ButtonWidget doneButton;
    private ButtonWidget addButton;
    private ButtonWidget clearButton;
    private ImageList list;
    private TextWidget titleWidget;

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
        // 使用 TextWidget，并修正 getWidth
        this.titleWidget = new TextWidget(TITLE, this.textRenderer);
        this.titleWidget.setX(this.width / 2 - this.textRenderer.getWidth(TITLE) / 2);
        this.titleWidget.setY(titleY);
        this.addDrawableChild(this.titleWidget);

        inputY = titleY + 30;
        this.pathInput = new TextFieldWidget(this.textRenderer, this.width / 2 - 110, inputY, 200, 20, Text.literal("输入图片路径"));
        this.pathInput.setMaxLength(Integer.MAX_VALUE);
        this.addDrawableChild(this.pathInput);

        this.browseButton = ButtonWidget.builder(
                Text.literal("..."), (btn) -> this.openFileChooser()
        ).position(this.width / 2 + 95, inputY).size(20, 20).tooltip(Tooltip.of(Text.translatable("overlayer.screen.button.select_file.tooltip"))).build();
        this.addDrawableChild(this.browseButton);

        int buttonRowY = this.height - 30;
        int totalWidth = BUTTON_WIDTH * 3 + 8;
        int startX = this.width / 2 - totalWidth / 2;

        this.doneButton = ButtonWidget.builder(DONE, (btn) -> this.close()).position(startX, buttonRowY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(this.doneButton);

        this.addButton = ButtonWidget.builder(ADD, (btn) -> this.addCurrentPath()).position(startX + BUTTON_WIDTH + 4, buttonRowY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(this.addButton);

        this.clearButton = ButtonWidget.builder(CLEAR, (btn) -> this.clearList()).position(startX + (BUTTON_WIDTH + 4) * 2, buttonRowY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addDrawableChild(this.clearButton);

        // 创建 ImageList
        this.list = new ImageList(this.client, 0, 0, 0, LIST_ENTRY_HEIGHT, this.textRenderer, this::removeEntry, this::openEditScreen);
        this.list.updateEntries(this.imageEntries);
        this.addDrawableChild(this.list);

        this.updateLayout();
    }

    private void openEditScreen(ImageEntry entry) {
        if (this.client != null) {
            this.client.setScreen(new ImageEditScreen(this, entry, (edited) -> {
                this.list.updateEntries(this.imageEntries);
                manager.save();
            }));
        }
    }

    @Override
    protected void initTabNavigation() {
        super.initTabNavigation();
        this.updateLayout();
    }

    private void updateLayout() {
        this.titleWidget.setX(this.width / 2 - this.textRenderer.getWidth(TITLE) / 2);
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

        this.list.setDimensions(listWidth, listHeight);
        this.list.setPosition(10, listTop);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.lastScreen);
        }
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    private void openFileChooser() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String result = selectFile(stack);
            if (result != null) {
                this.pathInput.setText(result);
            }
        }
    }

    private void addCurrentPath() {
        String path = this.pathInput.getText().trim();
        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        }
        if (path.isEmpty()) {
            OverlayerToast.showWarning(
                    Text.translatable("overlayer.toast.warning.invalid_path.title"), Text.translatable("overlayer.toast.warning.invalid_path.meg.empty_path")
            );
            return;
        }
        String extension = getFileExtension(path);
        if (extension.isEmpty()) {
            OverlayerToast.showWarning(
                    Text.translatable("overlayer.toast.warning.invalid_path.title"), Text.translatable("overlayer.toast.warning.invalid_path.meg.no_extension")
            );
            return;
        }
        if (!validFormat.contains(extension)) {
            OverlayerToast.showWarning(
                    Text.translatable("overlayer.toast.warning.unsupported_format.title"), Text.translatable("overlayer.toast.warning.unsupported_format.meg", extension)
            );
            return;
        }
        if (!fileExists(path)) {
            OverlayerToast.showWarning(
                    Text.translatable("overlayer.toast.warning.invalid_path.title"), Text.translatable("overlayer.toast.warning.invalid_path.meg.no_file")
            );
            return;
        }
        int maxId = imageEntries.stream().mapToInt(ImageEntry::getId).max().orElse(0);
        int newId = maxId + 1;
        ImageEntry newEntry = new ImageEntry(newId, path);
        imageEntries.add(newEntry);
        this.list.updateEntries(imageEntries);
        this.pathInput.setText("");
        manager.save();
    }

    private void removeEntry(ImageEntry entry) {
        if (this.client != null) {
            this.client.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            imageEntries.remove(entry);
                            this.list.updateEntries(imageEntries);
                            manager.save();
                        }
                        this.client.setScreen(this);
                    }, Text.translatable("overlayer.screen.delete_confirm.title"), Text.translatable("overlayer.screen.delete_confirm.meg"), Text.translatable("overlayer.screen.common.delete"), ScreenTexts.CANCEL
            ));
        }
    }

    private void clearList() {
        if (imageEntries.isEmpty()) {
            return;
        }
        if (this.client != null) {
            this.client.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            imageEntries.clear();
                            this.list.updateEntries(imageEntries);
                            manager.save();
                        }
                        this.client.setScreen(this);
                    }, Text.translatable("overlayer.screen.delete_all_confirm.title"), Text.translatable("overlayer.screen.delete_all_confirm.meg"), Text.translatable("overlayer.screen.common.delete_all"), ScreenTexts.CANCEL
            ));
        }
    }
}
