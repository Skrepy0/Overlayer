package com.skrepy.overlayer.client.gui;

import static com.skrepy.overlayer.manager.OverlayerManager.selectFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

@OnlyIn(Dist.CLIENT)
public class ImageEditScreen extends Screen {
    private static final Component TITLE = Component.translatable("overlayer.screen.image_edit.title");
    private static final Component SAVE = Component.translatable("overlayer.screen.common.save");
    private static final Component CANCEL = Component.translatable("overlayer.screen.common.cancel");

    private final Screen lastScreen;
    private final ImageEntry entry;
    private final Consumer<ImageEntry> onSave;

    // 右侧控件
    private EditBox pathInput;
    private Button browseButton;
    private ExtendedSlider xSlider;
    private ExtendedSlider ySlider;
    private ExtendedSlider scaleSlider;
    private ExtendedSlider alphaSlider;
    private EditBox layerInput;
    private Button modeButton;
    private Button saveButton;
    private Button cancelButton;

    private int modeIndex = 0;
    private static final MutableComponent[] MODES = {Component.translatable("overlayer.screen.image_edit.button.mode.always"), Component.translatable("overlayer.screen.image_edit.button.mode.ingame"), Component.translatable("overlayer.screen.image_edit.button.mode.not_ingame"), Component.translatable("overlayer.screen.image_edit.button.mode.disable")};
    private static final String[] MODE_VALUES = {"always", "ingame", "not_ingame", "disabled"};

    // 预览相关
    private int previewSize = 150;
    private int previewX, previewY;

    // 缓存预览图片尺寸和文件状态
    private String currentPreviewPath = null;
    private Dimension currentPreviewDimension = null;
    private boolean previewFileExists = true;   // 标记文件是否存在，避免反复尝试

    private final DecimalFormat df = new DecimalFormat("0.00");

    public ImageEditScreen(Screen lastScreen, ImageEntry entry, Consumer<ImageEntry> onSave) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.entry = entry;
        this.onSave = onSave;
        String currentMode = entry.getDisplayMode();
        for (int i = 0; i < MODE_VALUES.length; i++) {
            if (MODE_VALUES[i].equals(currentMode)) {
                modeIndex = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        int screenWidth = this.width;
        int screenHeight = this.height;

        // ---- 左右分栏布局 ----
        int leftWidth = (int) (screenWidth * 0.45);
        int rightWidth = screenWidth - leftWidth - 20;
        if (rightWidth < 200) {
            leftWidth = screenWidth - 220;
            rightWidth = 200;
        }
        if (leftWidth < 80) {
            leftWidth = 80;
            rightWidth = screenWidth - leftWidth - 20;
        }

        int leftStartX = 10;
        int rightStartX = leftStartX + leftWidth + 20;

        // 标题
        int titleY = 10;
        StringWidget titleWidget = new StringWidget(TITLE, this.font);
        titleWidget.setX((screenWidth - this.font.width(TITLE)) / 2);
        titleWidget.setY(titleY);
        this.addRenderableWidget(titleWidget);

        // ---- 预览 ----
        int previewMaxSize = Math.min(leftWidth - 20, screenHeight - 70);
        previewSize = Math.max(80, Math.min(300, previewMaxSize));
        previewX = leftStartX + (leftWidth - previewSize) / 2;
        previewY = (screenHeight - previewSize) / 2;
        if (previewY < 30) previewY = 30;

        // ---- 右栏控件 ----
        int startY = 30;
        int rightMargin = 30;
        int spacing = 6;

        // 1) 路径输入 + 浏览按钮
        int pathWidth = rightWidth - 30 - 54;
        this.pathInput = new EditBox(this.font, rightStartX + rightMargin, startY, pathWidth, 20, Component.literal("图片路径"));
        this.pathInput.setMaxLength(Integer.MAX_VALUE);
        this.pathInput.setValue(entry.getPath());
        this.addRenderableWidget(this.pathInput);

        this.browseButton = Button.builder(Component.literal("..."), (btn) -> this.openFileChooser()).pos(rightStartX + rightMargin + pathWidth + 4, startY).size(20, 20).build();
        this.addRenderableWidget(this.browseButton);

        // 2) 滑块
        int sliderY = startY + 28;
        int sliderWidth = rightWidth - 2 * rightMargin;

        this.xSlider = new XSlider(rightStartX + rightMargin, sliderY, sliderWidth, 20, Component.literal("X: "), Component.literal(" %"), -200, 200, entry.getXOffset(), 1, 0, true);
        this.addRenderableWidget(this.xSlider);

        sliderY += 20 + spacing;
        this.ySlider = new YSlider(rightStartX + rightMargin, sliderY, sliderWidth, 20, Component.literal("Y: "), Component.literal(" %"), -200, 200, entry.getYOffset(), 1, 0, true);
        this.addRenderableWidget(this.ySlider);

        sliderY += 20 + spacing;
        this.scaleSlider = new ScaleSlider(rightStartX + rightMargin, sliderY, sliderWidth, 20, Component.literal("缩放: "), Component.literal(""), 1, 300, (int) (entry.getScale() * 100), 1, 0, false);
        this.addRenderableWidget(this.scaleSlider);

        sliderY += 20 + spacing;
        this.alphaSlider = new AlphaSlider(rightStartX + rightMargin, sliderY, sliderWidth, 20, Component.literal("透明度: "), Component.literal(""), 1, 100, (int) (entry.getAlpha() * 100), 1, 0, false);
        this.addRenderableWidget(this.alphaSlider);

        // 3) 图层标签
        sliderY += 20 + spacing + 4;
        int rowWidth = sliderWidth;
        StringWidget layerLabel = new StringWidget(Component.translatable("overlayer.screen.image_edit.label.layer"), this.font);
        layerLabel.setX(rightStartX + rightMargin);
        layerLabel.setY(sliderY + 2);
        this.addRenderableWidget(layerLabel);

        // 4) 图层输入框
        sliderY += 20 + spacing;
        this.layerInput = new EditBox(this.font, rightStartX + rightMargin, sliderY, rowWidth, 20, Component.literal("图层"));
        this.layerInput.setValue(String.valueOf(entry.getLayer()));
        this.layerInput.setFilter(s -> s.matches("\\d*"));
        this.layerInput.setMaxLength(6);
        this.addRenderableWidget(this.layerInput);

        // 5) 模式按钮
        sliderY += 20 + spacing;
        this.modeButton = Button.builder(Component.translatable("overlayer.screen.image_edit.button.mode").append(MODES[modeIndex]), (btn) -> this.cycleMode()).pos(rightStartX + rightMargin, sliderY).size(rowWidth, 20).build();
        this.addRenderableWidget(this.modeButton);

        // 6) 底部按钮
        int buttonY = screenHeight - 30;
        int btnWidth = Math.min(100, (rightWidth - 20) / 2);
        int btnSpacing = 10;
        int totalBtnWidth = btnWidth * 2 + btnSpacing;
        int btnStartX = rightStartX + (rightWidth - totalBtnWidth) / 2;

        this.saveButton = Button.builder(SAVE, (btn) -> this.saveAndClose()).pos(btnStartX, buttonY).size(btnWidth, 20).build();
        this.addRenderableWidget(this.saveButton);

        this.cancelButton = Button.builder(CANCEL, (btn) -> this.cancel()).pos(btnStartX + btnWidth + btnSpacing, buttonY).size(btnWidth, 20).build();
        this.addRenderableWidget(this.cancelButton);

        // 预加载预览尺寸
        loadPreviewDimension(entry.getPath());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 检测路径是否变化，若变化则重新加载（即使文件不存在，也会更新缓存状态）
        String currentPath = this.pathInput.getValue();
        if (!currentPath.equals(currentPreviewPath)) {
            loadPreviewDimension(currentPath);
        }

        // 绘制预览
        ResourceLocation texture = null;
        if (this.minecraft != null) {
            texture = entry.getCurrentFrame(this.minecraft.getTextureManager(), 0);
        }

        // 如果纹理有效且我们缓存了尺寸，则绘制图片
        if (texture != null && currentPreviewDimension != null && previewFileExists) {
            int imgWidth = currentPreviewDimension.width;
            int imgHeight = currentPreviewDimension.height;

            double scaleX = (double) previewSize / imgWidth;
            double scaleY = (double) previewSize / imgHeight;
            double scale = Math.min(scaleX, scaleY);
            int drawWidth = (int) (imgWidth * scale);
            int drawHeight = (int) (imgHeight * scale);
            int drawX = previewX + (previewSize - drawWidth) / 2;
            int drawY = previewY + (previewSize - drawHeight) / 2;

            guiGraphics.blit(texture, drawX, drawY, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
        } else {
            // 占位符：显示相应提示
            guiGraphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF888888);
            String message;
            if (currentPreviewPath != null && !previewFileExists) {
                message = "文件不存在";
            } else {
                message = Component.translatable("overlayer.screen.image_edit.label.preview").getString();
            }
            guiGraphics.drawString(this.font, message, previewX + previewSize / 2 - this.font.width(message) / 2, previewY + previewSize / 2 - 4, 0xFFFFFF);
        }
    }

    private void cycleMode() {
        modeIndex = (modeIndex + 1) % MODES.length;
        this.modeButton.setMessage(Component.translatable("overlayer.screen.image_edit.button.mode").append(MODES[modeIndex]));
        entry.setDisplayMode(MODE_VALUES[modeIndex]);
    }

    private void openFileChooser() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String result = selectFile(stack);
            if (result != null) {
                this.pathInput.setValue(result);
                entry.setPath(result);
                entry.clearCache();
                loadPreviewDimension(result);
            }
        }
    }

    private void saveAndClose() {
        String path = this.pathInput.getValue().trim();
        if (path.isEmpty()) {
            Overlayer.LOGGER.warn("路径不能为空，保存取消");
            return;
        }
        if (!entry.getPath().equals(path)) {
            entry.setPath(path);
            entry.clearCache();
        }

        entry.setXOffset((int) xSlider.getValue());
        entry.setYOffset((int) ySlider.getValue());
        entry.setScale(scaleSlider.getValue() / 100.0);
        entry.setAlpha(alphaSlider.getValue() / 100.0);

        try {
            int layer = Integer.parseInt(layerInput.getValue().trim());
            if (layer < 0) layer = 0;
            entry.setLayer(layer);
        } catch (NumberFormatException e) {
            entry.setLayer(0);
        }

        entry.setDisplayMode(MODE_VALUES[modeIndex]);
        OverlayerManager.getInstance().save();
        onSave.accept(entry);
        if (this.minecraft != null) {
            this.minecraft.setScreen(lastScreen);
        }
    }

    private void cancel() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(lastScreen);
        }
    }

    @Override
    public void onClose() {
        this.cancel();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    // ---------- 预览尺寸加载（核心改进） ----------
    private void loadPreviewDimension(String path) {
        // 1. 空路径处理
        if (path == null || path.isEmpty()) {
            currentPreviewPath = null;
            currentPreviewDimension = null;
            previewFileExists = false;
            return;
        }

        // 2. 如果路径相同且文件已标记为不存在，则直接返回（避免反复报错）
        if (path.equals(currentPreviewPath) && !previewFileExists) {
            return;
        }

        // 3. 使用 Path 检查文件存在性和可读性
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            // 文件不存在或不可读，更新缓存
            currentPreviewPath = path;
            currentPreviewDimension = null;
            previewFileExists = false;
            Overlayer.LOGGER.debug("预览文件不存在或不可读: {}", path);
            return;
        }

        // 4. 尝试读取图片尺寸
        try (InputStream is = Files.newInputStream(filePath)) {
            BufferedImage img = ImageIO.read(is);
            if (img != null) {
                currentPreviewDimension = new Dimension(img.getWidth(), img.getHeight());
                currentPreviewPath = path;
                previewFileExists = true;
                Overlayer.LOGGER.debug("成功加载预览图片尺寸: {} x {}", img.getWidth(), img.getHeight());
            } else {
                // ImageIO 无法解码，但文件存在
                currentPreviewDimension = null;
                previewFileExists = false;
                Overlayer.LOGGER.warn("无法解码图片文件: {}", path);
            }
        } catch (Exception e) {
            currentPreviewDimension = null;
            previewFileExists = false;
            Overlayer.LOGGER.debug("加载预览图片尺寸失败: {}", path, e);
        }
    }

    // ---------- 自定义滑动条内部类（保持不变） ----------
    private class XSlider extends ExtendedSlider {
        public XSlider(int x, int y, int width, int height, Component prefix, Component suffix, int minValue, int maxValue, int currentValue, int stepSize, int precision, boolean drawString) {
            super(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, stepSize, precision, drawString);
        }

        @Override
        protected void applyValue() {
            entry.setXOffset((int) this.getValue());
        }
    }

    private class YSlider extends ExtendedSlider {
        public YSlider(int x, int y, int width, int height, Component prefix, Component suffix, int minValue, int maxValue, int currentValue, int stepSize, int precision, boolean drawString) {
            super(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, stepSize, precision, drawString);
        }

        @Override
        protected void applyValue() {
            entry.setYOffset((int) this.getValue());
        }
    }

    private class ScaleSlider extends ExtendedSlider {
        public ScaleSlider(int x, int y, int width, int height, Component prefix, Component suffix, int minValue, int maxValue, int currentValue, int stepSize, int precision, boolean drawString) {
            super(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, stepSize, precision, drawString);
            updateMessage();
        }

        @Override
        protected void applyValue() {
            entry.setScale(this.getValue() / 100.0);
        }

        @Override
        protected void updateMessage() {
            double val = getValue() / 100.0;
            setMessage(Component.translatable("overlayer.screen.image_edit.slide.zoom").append(df.format(val) + "x"));
        }
    }

    private class AlphaSlider extends ExtendedSlider {
        public AlphaSlider(int x, int y, int width, int height, Component prefix, Component suffix, int minValue, int maxValue, int currentValue, int stepSize, int precision, boolean drawString) {
            super(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, stepSize, precision, drawString);
            updateMessage();
        }

        @Override
        protected void applyValue() {
            entry.setAlpha(this.getValue() / 100.0);
        }

        @Override
        protected void updateMessage() {
            double val = getValue() / 100.0;
            setMessage(Component.translatable("overlayer.screen.image_edit.slide.alpha").append(df.format(val)));
        }
    }
}
