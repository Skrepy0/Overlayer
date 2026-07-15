package com.skrepy.overlayer.client.gui;

import static com.skrepy.overlayer.Overlayer.validFormat;
import static com.skrepy.overlayer.manager.OverlayerManager.*;
import static net.minecraft.screen.ScreenTexts.DONE;

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
import com.skrepy.overlayer.client.gui.components.ScrollablePanel;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ImageEditScreen extends Screen {
    private static final Text TITLE = Text.translatable("overlayer.screen.image_edit.title");
    private static final MutableText[] MODES = {Text.translatable("overlayer.screen.image_edit.button.mode.always"), Text.translatable("overlayer.screen.image_edit.button.mode.ingame"), Text.translatable("overlayer.screen.image_edit.button.mode.not_ingame"), Text.translatable("overlayer.screen.image_edit.button.mode.disable")
    };
    private static final String[] MODE_VALUES = {"always", "ingame", "not_ingame", "disabled"};
    private final Screen lastScreen;
    private final ImageEntry entry;
    private final Consumer<ImageEntry> onSave;
    private final DecimalFormat df = new DecimalFormat("0.00");
    // 控件
    private TextFieldWidget pathInput;
    private ButtonWidget browseButton;
    private XSlider xSlider;        // 改为具体子类类型
    private YSlider ySlider;
    private RotationSlider rotationSlider;
    private ScaleSlider scaleSlider;
    private AlphaSlider alphaSlider;
    private TextFieldWidget layerInput;
    private ButtonWidget modeButton;
    private ButtonWidget doneButton;
    private int modeIndex = 0;
    ScrollablePanel scrollPanel;
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
        TextWidget titleWidget = new TextWidget(TITLE, this.textRenderer);
        titleWidget.setX((screenWidth - this.textRenderer.getWidth(TITLE)) / 2);
        titleWidget.setY(titleY);
        this.addDrawableChild(titleWidget);

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

        // 计算面板内容总高度（用于滚动）
        int contentHeight = 0;
        contentHeight += 28; // 路径输入 + 浏览按钮
        contentHeight += spacing;
        for (int i = 0; i < 5; i++) { // 5个滑块
            contentHeight += 20 + spacing;
        }
        contentHeight += 4 + 20 + spacing; // 图层标签和输入框
        contentHeight += 20 + spacing;
        contentHeight += 20 + spacing * 2; // 模式按钮和额外间距

        // 创建滚动面板
        this.scrollPanel = new ScrollablePanel(panelX, panelY, panelWidth, panelHeight, contentHeight);
        this.addDrawableChild(this.scrollPanel);

        // ---- 在面板中添加控件（坐标相对于面板内部） ----
        int offsetY = 0;

        // 1) 路径输入 + 浏览按钮
        int pathWidth = panelWidth - 24;
        this.pathInput = new TextFieldWidget(this.textRenderer, 0, offsetY, pathWidth, 20, Text.literal("图片路径"));
        this.pathInput.setMaxLength(Integer.MAX_VALUE);
        this.pathInput.setText(entry.getPath());
        this.scrollPanel.addWidget(this.pathInput);

        this.browseButton = ButtonWidget.builder(
                Text.literal("..."), (btn) -> this.openFileChooser()
        ).position(pathWidth + 4, offsetY).size(20, 20).tooltip(Tooltip.of(Text.translatable("overlayer.screen.button.select_file.tooltip"))).build();
        this.scrollPanel.addWidget(this.browseButton);

        offsetY += 28 + spacing;

        // 2) 滑块（使用自定义滑块类，需确保它们继承自 AbstractSliderWidget 或类似）
        this.xSlider = new XSlider(0, offsetY, panelWidth, 20, Text.literal("X: "), Text.literal(" %"), -200, 200, entry.getXOffset());
        this.scrollPanel.addWidget(this.xSlider);
        offsetY += 20 + spacing;

        this.ySlider = new YSlider(0, offsetY, panelWidth, 20, Text.literal("Y: "), Text.literal(" %"), -200, 200, entry.getYOffset());
        this.scrollPanel.addWidget(this.ySlider);
        offsetY += 20 + spacing;

        this.rotationSlider = new RotationSlider(0, offsetY, panelWidth, 20, Text.translatable("overlayer.screen.image_edit.slide.rotation"), Text.literal("°"), -360, 360, entry.getRotation());
        this.scrollPanel.addWidget(this.rotationSlider);
        offsetY += 20 + spacing;

        this.scaleSlider = new ScaleSlider(0, offsetY, panelWidth, 20, Text.translatable("overlayer.screen.image_edit.slide.zoom"), Text.literal(""), 1, 300, (int) (entry.getScale() * 100));
        this.scrollPanel.addWidget(this.scaleSlider);
        offsetY += 20 + spacing;

        this.alphaSlider = new AlphaSlider(0, offsetY, panelWidth, 20, Text.translatable("overlayer.screen.image_edit.slide.alpha"), Text.literal(""), 1, 100, (int) (entry.getAlpha() * 100));
        this.scrollPanel.addWidget(this.alphaSlider);
        offsetY += 20 + spacing + 4;

        // 3) 图层标签
        TextWidget layerLabel = new TextWidget(Text.translatable("overlayer.screen.image_edit.label.layer"), this.textRenderer);
        layerLabel.setX(0);
        layerLabel.setY(offsetY + 2);
        this.scrollPanel.addWidget(layerLabel);
        offsetY += 20 + spacing;

        // 4) 图层输入框
        this.layerInput = new TextFieldWidget(this.textRenderer, 0, offsetY, panelWidth, 20, Text.literal("图层"));
        this.layerInput.setText(String.valueOf(entry.getLayer()));
        this.layerInput.setTextPredicate(s -> s.matches("\\d*"));
        this.layerInput.setMaxLength(6);
        this.scrollPanel.addWidget(this.layerInput);
        offsetY += 20 + spacing;

        // 5) 模式按钮
        this.modeButton = ButtonWidget.builder(
                Text.translatable("overlayer.screen.image_edit.button.mode").append(MODES[modeIndex]), (btn) -> this.cycleMode()
        ).position(0, offsetY).size(panelWidth, 20).build();
        this.scrollPanel.addWidget(this.modeButton);

        // ---- 6) 底部“完成”按钮（独立于面板） ----
        int buttonY = screenHeight - 40;
        int btnWidth = rightWidth - 20;
        int btnStartX = rightStartX + (rightWidth - btnWidth) / 2 + 20;

        this.doneButton = ButtonWidget.builder(
                Text.literal("完成"), (btn) -> this.saveAndClose()
        ).position(btnStartX, buttonY).size(btnWidth, 20).build();
        this.addDrawableChild(this.doneButton);

        // 加载预览尺寸
        loadPreviewDimension(entry.getAbsolutePath().toString());
    }

    // ========== 渲染 ==========
    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(context, mouseX, mouseY, partialTick);
        super.render(context, mouseX, mouseY, partialTick);

        String currentPath = this.pathInput.getText();
        if (!currentPath.equals(currentPreviewPath)) {
            loadPreviewDimension(currentPath);
        }

        // 绘制预览
        Identifier texture = null; // 使用 Identifier
        if (this.client != null) {
            texture = entry.getCurrentFrame(this.client.getTextureManager(), 0);
        }

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

            context.drawTexture(texture, drawX, drawY, 0, 0, drawWidth, drawHeight, drawWidth, drawHeight);
        } else {
            context.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF888888);
            String message;
            if (currentPreviewPath != null && !previewFileExists) {
                message = Text.translatable("overlayer.toast.warning.invalid_path.meg.no_file").getString();
            } else {
                message = Text.translatable("overlayer.screen.image_edit.label.preview").getString();
            }
            context.drawText(this.textRenderer, message, previewX + previewSize / 2 - this.textRenderer.getWidth(message) / 2, previewY + previewSize / 2 - 4, 0xFFFFFF, false);
        }
    }

    // ========== 交互事件 ==========

    private void cycleMode() {
        modeIndex = (modeIndex + 1) % MODES.length;
        this.modeButton.setMessage(Text.translatable("overlayer.screen.image_edit.button.mode").append(MODES[modeIndex]));
        entry.setDisplayMode(MODE_VALUES[modeIndex]);
    }

    private void openFileChooser() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String result = selectFile(stack);
            if (result != null) {
                this.pathInput.setText(result);
                entry.setPath(result);
                entry.clearCache();
                loadPreviewDimension(result);
            }
        }
    }

    private void saveAndClose() {
        String path = this.pathInput.getText().trim();
        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        }
        String extension = getFileExtension(path);

        // ====== 验证输入 ======
        if (path.isEmpty()) {
            OverlayerToast.showWarning(Text.translatable("overlayer.toast.warning.invalid_path.title"), Text.translatable("overlayer.toast.warning.invalid_path.meg.empty_path"));
            return;
        }
        if (extension.isEmpty()) {
            OverlayerToast.showWarning(Text.translatable("overlayer.toast.warning.invalid_path.title"), Text.translatable("overlayer.toast.warning.invalid_path.meg.no_extension"));
            return;
        }
        if (!validFormat.contains(extension)) {
            OverlayerToast.showWarning(Text.translatable("overlayer.toast.warning.unsupported_format.title"), Text.translatable("overlayer.toast.warning.unsupported_format.meg", extension));
            return;
        }
        if (!fileExists(path)) {
            OverlayerToast.showWarning(Text.translatable("overlayer.toast.warning.invalid_path.title"), Text.translatable("overlayer.toast.warning.invalid_path.meg.no_file"));
            return;
        }

        // ====== 应用更改 ======
        if (!entry.getPath().equals(path)) {
            entry.setPath(path);
            entry.clearCache();
        }

        entry.setXOffset((int) this.xSlider.getValue());
        entry.setYOffset((int) this.ySlider.getValue());
        entry.setRotation((int) this.rotationSlider.getValue());
        entry.setScale(this.scaleSlider.getValue() / 100.0);
        entry.setAlpha(this.alphaSlider.getValue() / 100.0);

        try {
            int layer = Integer.parseInt(this.layerInput.getText().trim());
            if (layer < 0) layer = 0;
            entry.setLayer(layer);
        } catch (NumberFormatException e) {
            entry.setLayer(0);
        }

        entry.setDisplayMode(MODE_VALUES[modeIndex]);

        OverlayerManager.getInstance().save();
        onSave.accept(entry);

        if (this.client != null) {
            this.client.setScreen(lastScreen);
        }
    }

    private void cancel() {
        if (this.client == null) return;
        this.client.setScreen(lastScreen);
    }

    @Override
    public void close() {
        this.cancel();
    }

    @Override
    public boolean shouldPause() {
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
        if (path.equals(currentPreviewPath) && !previewFileExists) {
            return;
        }
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            currentPreviewPath = path;
            currentPreviewDimension = null;
            previewFileExists = false;
            Overlayer.LOGGER.debug("预览文件不存在或不可读: {}", path);
            return;
        }
        try (InputStream is = Files.newInputStream(filePath)) {
            BufferedImage img = ImageIO.read(is);
            if (img != null) {
                currentPreviewDimension = new Dimension(img.getWidth(), img.getHeight());
                currentPreviewPath = path;
                previewFileExists = true;
                Overlayer.LOGGER.debug("成功加载预览图片尺寸: {} x {}", img.getWidth(), img.getHeight());
            } else {
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

    // ========== 自定义滑块内部类 ==========
    private abstract static class AbstractCustomSlider extends SliderWidget {
        protected final Text prefix;
        protected final Text suffix;
        protected final int min;
        protected final int max;
        protected final DecimalFormat format;

        public AbstractCustomSlider(int x, int y, int width, int height, Text prefix, Text suffix, int min, int max, int currentValue) {
            super(x, y, width, height, Text.literal(""), 0.0); // 占位，后面覆盖
            this.prefix = prefix;
            this.suffix = suffix;
            this.min = min;
            this.max = max;
            this.format = new DecimalFormat("0");
            // 计算初始 value (0~1)
            this.value = (currentValue - min) / (double) (max - min);
            updateMessage();
        }

        @Override
        protected abstract void applyValue();

        @Override
        protected void updateMessage() {
            int val = (int) getValue();
            setMessage(Text.literal(prefix.getString() + format.format(val) + suffix.getString()));
        }

        public double getValue() {
            return min + (max - min) * this.value;
        }

        protected void setValue(double v) {
            this.value = (v - min) / (double) (max - min);
        }
    }

    private class XSlider extends AbstractCustomSlider {
        public XSlider(int x, int y, int width, int height, Text prefix, Text suffix, int min, int max, int currentValue) {
            super(x, y, width, height, prefix, suffix, min, max, currentValue);
            this.value = (currentValue - min) / (double) (max - min);
            updateMessage();
        }

        @Override
        protected void applyValue() {
            entry.setXOffset((int) getValue());
        }
    }

    private class YSlider extends AbstractCustomSlider {
        public YSlider(int x, int y, int width, int height, Text prefix, Text suffix, int min, int max, int currentValue) {
            super(x, y, width, height, prefix, suffix, min, max, currentValue);
            this.value = (currentValue - min) / (double) (max - min);
            updateMessage();
        }

        @Override
        protected void applyValue() {
            entry.setYOffset((int) getValue());
        }
    }

    private class RotationSlider extends AbstractCustomSlider {
        public RotationSlider(int x, int y, int width, int height, Text prefix, Text suffix, int min, int max, int currentValue) {
            super(x, y, width, height, prefix, suffix, min, max, currentValue);
            this.value = (currentValue - min) / (double) (max - min);
            updateMessage();
        }

        @Override
        protected void applyValue() {
            entry.setRotation((int) getValue());
        }
    }

    private class ScaleSlider extends AbstractCustomSlider {
        public ScaleSlider(int x, int y, int width, int height, Text prefix, Text suffix, int min, int max, int currentValue) {
            super(x, y, width, height, prefix, suffix, min, max, currentValue);
            this.value = (currentValue - min) / (double) (max - min);
            updateMessage();
        }

        @Override
        protected void applyValue() {
            entry.setScale(getValue() / 100.0);
        }

        @Override
        protected void updateMessage() {
            double val = getValue() / 100.0;
            setMessage(Text.translatable("overlayer.screen.image_edit.slide.zoom").append(df.format(val) + "x"));
        }
    }

    private class AlphaSlider extends AbstractCustomSlider {
        public AlphaSlider(int x, int y, int width, int height, Text prefix, Text suffix, int min, int max, int currentValue) {
            super(x, y, width, height, prefix, suffix, min, max, currentValue);
            this.value = (currentValue - min) / (double) (max - min);
            updateMessage();
        }

        @Override
        protected void applyValue() {
            entry.setAlpha(getValue() / 100.0);
        }

        @Override
        protected void updateMessage() {
            double val = getValue() / 100.0;
            setMessage(Text.translatable("overlayer.screen.image_edit.slide.alpha").append(df.format(val)));
        }
    }
}
