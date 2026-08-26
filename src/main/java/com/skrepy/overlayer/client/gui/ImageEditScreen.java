package com.skrepy.overlayer.client.gui;

import static com.skrepy.overlayer.Overlayer.validFormat;
import static com.skrepy.overlayer.manager.OverlayerManager.selectFile;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

import com.skrepy.overlayer.client.gui.components.ScrollablePanel;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

public class ImageEditScreen extends Screen {
    private static final Component TITLE = Component.translatable("overlayer.screen.image_edit.title");
    private static final MutableComponent[] MODES = {Component.translatable("overlayer.screen.image_edit.button.mode.always"), Component.translatable("overlayer.screen.image_edit.button.mode.ingame"), Component.translatable("overlayer.screen.image_edit.button.mode.not_ingame"), Component.translatable("overlayer.screen.image_edit.button.mode.disable")
    };
    private static final String[] MODE_VALUES = {"always", "ingame", "not_ingame", "disabled"};
    private final Screen lastScreen;
    private final ImageEntry entry;
    private final Consumer<ImageEntry> onSave;
    private final DecimalFormat df = new DecimalFormat("0.00");
    // 预览加载状态（避免重复触发）
    private final AtomicBoolean previewLoading = new AtomicBoolean(false);
    // 控件
    private EditBox pathInput;
    private Button browseButton;
    private ExtendedSlider xSlider;
    private ExtendedSlider ySlider;
    private ExtendedSlider rotationSlider;
    private ExtendedSlider scaleSlider;
    private ExtendedSlider alphaSlider;
    private EditBox layerInput;
    private Button modeButton;
    private Button doneButton;
    private int modeIndex = 0;
    private ScrollablePanel scrollPanel;
    // 预览相关
    private int previewSize = 150;
    private int previewX, previewY;
    // 缓存预览图片尺寸和文件状态
    private String currentPreviewPath = null;
    private Dimension currentPreviewDimension = null;
    private boolean previewFileExists = true;

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

    // ========== 工具方法 ==========
    private static String getFileExtension(String path) {
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < path.length() - 1) {
            return path.substring(dotIdx + 1).toLowerCase();
        }
        return "";
    }

    private static boolean fileExists(String path) {
        return Files.exists(Paths.get(path));
    }

    // ========== 交互事件 ==========

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
        int rightStartX = leftStartX + leftWidth - 15;

        // ---- 标题 ----
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

        // ---- 右栏滚动面板 ----
        int panelX = rightStartX + 30;
        int panelY = 30;
        int panelWidth = rightWidth - 20;
        int panelHeight = screenHeight - 30 - 30 - 30; // 留出底部按钮空间
        int spacing = 6;

        // 计算面板内容总高度
        int contentHeight = 0;
        // 路径输入 + 浏览按钮（一行）
        contentHeight += 28;
        contentHeight += spacing;
        // 5个滑块：X, Y, 旋转, 缩放, Alpha
        for (int i = 0; i < 5; i++) {
            contentHeight += 20 + spacing;
        }
        contentHeight += 4; // 标签上方间距
        contentHeight += 20; // 标签高度
        contentHeight += spacing;
        contentHeight += 20; // 图层输入框
        contentHeight += spacing;
        contentHeight += 20; // 模式按钮
        contentHeight += spacing * 2;

        // 创建滚动面板
        this.scrollPanel = new ScrollablePanel(panelX, panelY, panelWidth, panelHeight, contentHeight);
        this.addRenderableWidget(this.scrollPanel);

        // ---- 在面板中添加控件（坐标相对于面板内部） ----
        int offsetY = 0;

        // 1) 路径输入 + 浏览按钮
        int pathWidth = panelWidth - 24;
        this.pathInput = new EditBox(this.font, 0, offsetY, pathWidth, 20, Component.literal("图片路径"));
        this.pathInput.setMaxLength(Integer.MAX_VALUE);
        this.pathInput.setValue(entry.getPath());
        this.scrollPanel.addWidget(this.pathInput);

        this.browseButton = Button.builder(Component.literal("..."), (_) -> this.openFileChooser()).pos(pathWidth + 4, offsetY).size(20, 20).tooltip(Tooltip.create(Component.translatable("overlayer.screen.button.select_file.tooltip"))).build();
        this.scrollPanel.addWidget(this.browseButton);

        offsetY += 28 + spacing;

        // 2) 滑块
        this.xSlider = new XSlider(0, offsetY, panelWidth, 20, Component.literal("X: "), Component.literal(" %"), -200, 200, entry.getXOffset(), 1, 0, true);
        this.scrollPanel.addWidget(this.xSlider);
        offsetY += 20 + spacing;

        this.ySlider = new YSlider(0, offsetY, panelWidth, 20, Component.literal("Y: "), Component.literal(" %"), -200, 200, entry.getYOffset(), 1, 0, true);
        this.scrollPanel.addWidget(this.ySlider);
        offsetY += 20 + spacing;

        this.rotationSlider = new RotationSlider(0, offsetY, panelWidth, 20, Component.translatable("overlayer.screen.image_edit.slide.rotation"), Component.literal("°"), -360, 360, entry.getRotation(), 1, 0, true);
        this.scrollPanel.addWidget(this.rotationSlider);
        offsetY += 20 + spacing;

        this.scaleSlider = new ScaleSlider(0, offsetY, panelWidth, 20, Component.translatable("overlayer.screen.image_edit.slide.zoom"), Component.literal(""), 1, 300, (int) (entry.getScale() * 100), 1, 0, false);
        this.scrollPanel.addWidget(this.scaleSlider);
        offsetY += 20 + spacing;

        this.alphaSlider = new AlphaSlider(0, offsetY, panelWidth, 20, Component.translatable("overlayer.screen.image_edit.slide.alpha"), Component.literal(""), 1, 100, (int) (entry.getAlpha() * 100), 1, 0, false);
        this.scrollPanel.addWidget(this.alphaSlider);
        offsetY += 20 + spacing + 4;

        // 3) 图层标签
        StringWidget layerLabel = new StringWidget(Component.translatable("overlayer.screen.image_edit.label.layer"), this.font);
        layerLabel.setX(0);
        layerLabel.setY(offsetY + 2);
        this.scrollPanel.addWidget(layerLabel);
        offsetY += 20 + spacing;

        // 4) 图层输入框
        this.layerInput = new EditBox(this.font, 0, offsetY, panelWidth, 20, Component.literal("图层"));
        this.layerInput.setValue(String.valueOf(entry.getLayer()));
        this.layerInput.setFilter(s -> s.matches("\\d*"));
        this.layerInput.setMaxLength(6);
        this.scrollPanel.addWidget(this.layerInput);
        offsetY += 20 + spacing;

        // 5) 模式按钮
        this.modeButton = Button.builder(
                Component.translatable("overlayer.screen.image_edit.button.mode").append(MODES[modeIndex]), (_) -> this.cycleMode()
        ).pos(0, offsetY).size(panelWidth, 20).build();
        this.scrollPanel.addWidget(this.modeButton);

        // 6) 底部按钮
        int buttonY = screenHeight - 40;
        int btnWidth = rightWidth - 20;
        int btnStartX = rightStartX + (rightWidth - btnWidth) / 2 + 20;

        this.doneButton = Button.builder(CommonComponents.GUI_DONE, (_) -> this.saveAndClose()).pos(btnStartX, buttonY).size(btnWidth, 20).build();
        this.addRenderableWidget(this.doneButton);

        loadPreviewDimension(entry.getAbsolutePath().toString());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.scrollPanel != null && this.scrollPanel.isMouseOver(mouseX, mouseY)) {
            return this.scrollPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ========== 渲染 ==========
    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. 绘制所有子控件
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        // 2. 更新预览路径
        String currentPath = this.pathInput.getValue();
        if (!currentPath.equals(currentPreviewPath)) {
            loadPreviewDimension(currentPath);
        }

        // 3. 绘制预览
        Identifier texture;
        texture = entry.getCurrentFrame(this.minecraft.getTextureManager(), 0);

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

            guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED, texture, drawX, drawY, 0.0f, 0.0f, drawWidth, drawHeight, drawWidth, drawHeight, drawWidth, drawHeight, -1
            );
        } else {
            guiGraphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF888888);
            String message;
            if (currentPreviewPath != null && !previewFileExists) {
                message = Component.translatable("overlayer.toast.warning.invalid_path.meg.no_file").getString();
            } else {
                message = Component.translatable("overlayer.screen.image_edit.label.preview").getString();
            }
            guiGraphics.text(this.font, message, previewX + previewSize / 2 - this.font.width(message) / 2, previewY + previewSize / 2 - 4, 0xFFFFFF, false);
        }
    }

    private void cycleMode() {
        modeIndex = (modeIndex + 1) % MODES.length;
        this.modeButton.setMessage(Component.translatable("overlayer.screen.image_edit.button.mode").append(MODES[modeIndex]));
        entry.setDisplayMode(MODE_VALUES[modeIndex]);
    }

    private void openFileChooser() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 假设 selectFile 存在（可能在其他类中实现），若报错请补充实现
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
        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        }
        String extension = getFileExtension(path);

        // ====== 验证输入 ======
        if (path.isEmpty()) {
            OverlayerToast.showWarning(Component.translatable("overlayer.toast.warning.invalid_path.title"), Component.translatable("overlayer.toast.warning.invalid_path.meg.empty_path"));
            return;
        }
        if (extension.isEmpty()) {
            OverlayerToast.showWarning(Component.translatable("overlayer.toast.warning.invalid_path.title"), Component.translatable("overlayer.toast.warning.invalid_path.meg.no_extension"));
            return;
        }
        if (!validFormat.contains(extension)) {
            OverlayerToast.showWarning(Component.translatable("overlayer.toast.warning.unsupported_format.title"), Component.translatable("overlayer.toast.warning.unsupported_format.meg", extension));
            return;
        }
        if (!fileExists(path)) {
            OverlayerToast.showWarning(Component.translatable("overlayer.toast.warning.invalid_path.title"), Component.translatable("overlayer.toast.warning.invalid_path.meg.no_file"));
            return;
        }

        // ====== 应用更改 ======
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

        this.minecraft.setScreenAndShow(lastScreen);
    }

    private void cancel() {
        this.minecraft.setScreenAndShow(lastScreen);
    }

    @Override
    public void onClose() {
        this.cancel();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    // ========== 预览尺寸加载 ==========
    private void loadPreviewDimension(String path) {
        if (path == null || path.isEmpty()) {
            currentPreviewPath = null;
            currentPreviewDimension = null;
            previewFileExists = false;
            return;
        }
        if (path.equals(currentPreviewPath)) {
            return;
        }
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            currentPreviewPath = path;
            currentPreviewDimension = null;
            previewFileExists = false;
            return;
        }
        // 避免重复加载
        if (!previewLoading.compareAndSet(false, true)) {
            return;
        }
        // 异步读取图片尺寸（仅读取头部元数据）
        CompletableFuture.runAsync(() -> {
            try (ImageInputStream iis = ImageIO.createImageInputStream(Files.newInputStream(filePath))) {
                if (iis == null) {
                    return;
                }
                java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    javax.imageio.ImageReader reader = readers.next();
                    try {
                        reader.setInput(iis, true, true);
                        int w = reader.getWidth(0);
                        int h = reader.getHeight(0);
                        if (w > 0 && h > 0) {
                            currentPreviewDimension = new Dimension(w, h);
                            currentPreviewPath = path;
                            previewFileExists = true;
                        }
                    } finally {
                        reader.dispose();
                    }
                }
            } catch (Exception e) {
                currentPreviewPath = path;
                currentPreviewDimension = null;
                previewFileExists = false;
            } finally {
                previewLoading.set(false);
            }
        });
    }

    // ========== 自定义滑块内部类（标记修改） ==========
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

    private class RotationSlider extends ExtendedSlider {
        public RotationSlider(int x, int y, int width, int height, Component prefix, Component suffix, int minValue, int maxValue, int currentValue, int stepSize, int precision, boolean drawString) {
            super(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, stepSize, precision, drawString);
        }

        @Override
        protected void applyValue() {
            entry.setRotation((int) this.getValue());
        }
    }

    private class ScaleSlider extends ExtendedSlider {
        private double lastVal = Double.MIN_VALUE;

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
            if (Math.abs(val - lastVal) > 0.001) {
                lastVal = val;
                setMessage(Component.translatable("overlayer.screen.image_edit.slide.zoom").append(df.format(val) + "x"));
            }
        }
    }

    private class AlphaSlider extends ExtendedSlider {
        private double lastVal = Double.MIN_VALUE;

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
            if (Math.abs(val - lastVal) > 0.001) {
                lastVal = val;
                setMessage(Component.translatable("overlayer.screen.image_edit.slide.alpha").append(df.format(val)));
            }
        }
    }
}
