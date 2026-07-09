package com.skrepy.overlayer.client.gui;

import static com.skrepy.overlayer.Overlayer.validFormat;
import static com.skrepy.overlayer.manager.OverlayerManager.*;

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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ImageEditScreen extends Screen {
    private static final Text TITLE = Text.translatable("overlayer.screen.image_edit.title");
    private static final Text SAVE = Text.translatable("overlayer.screen.common.save");
    private static final Text CANCEL = Text.translatable("overlayer.screen.common.cancel");

    private final Screen lastScreen;
    private final ImageEntry entry;
    private final Consumer<ImageEntry> onSave;

    // 控件
    private TextFieldWidget pathInput;
    private ButtonWidget browseButton;
    private XSlider xSlider;        // 改为具体子类类型
    private YSlider ySlider;
    private ScaleSlider scaleSlider;
    private AlphaSlider alphaSlider;
    private TextFieldWidget layerInput;
    private ButtonWidget modeButton;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;

    private int modeIndex = 0;
    private static final MutableText[] MODES = {Text.translatable("overlayer.screen.image_edit.button.mode.always"), Text.translatable("overlayer.screen.image_edit.button.mode.ingame"), Text.translatable("overlayer.screen.image_edit.button.mode.not_ingame"), Text.translatable("overlayer.screen.image_edit.button.mode.disable")
    };
    private static final String[] MODE_VALUES = {"always", "ingame", "not_ingame", "disabled"};

    // 预览相关
    private int previewSize = 150;
    private int previewX, previewY;

    // 缓存预览图片尺寸和文件状态
    private String currentPreviewPath = null;
    private Dimension currentPreviewDimension = null;
    private boolean previewFileExists = true;

    private boolean isChanged;

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
        isChanged = false;
    }

    @Override
    protected void init() {
        super.init();

        int screenWidth = this.width;
        int screenHeight = this.height;

        // ---- 左右分栏 ----
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

        // ---- 右栏控件 ----
        int startY = 30;
        int rightMargin = 30;
        int spacing = 6;

        // 1) 路径输入 + 浏览按钮
        int pathWidth = rightWidth - 30 - 54;
        this.pathInput = new TextFieldWidget(this.textRenderer, rightStartX + rightMargin, startY, pathWidth, 20, Text.literal("图片路径"));
        this.pathInput.setMaxLength(Integer.MAX_VALUE);
        this.pathInput.setText(entry.getPath());
        this.pathInput.setChangedListener(s -> {
            if (!s.equals(entry.getPath())) {
                isChanged = true;
            }
        });
        this.addDrawableChild(this.pathInput);

        this.browseButton = ButtonWidget.builder(
                Text.literal("..."), (btn) -> this.openFileChooser()
        ).position(rightStartX + rightMargin + pathWidth + 4, startY).size(20, 20).tooltip(Tooltip.of(Text.translatable("overlayer.screen.button.select_file.tooltip"))).build();
        this.addDrawableChild(this.browseButton);

        // 2) 滑块
        int sliderY = startY + 28;
        int sliderWidth = rightWidth - 2 * rightMargin;

        this.xSlider = new XSlider(rightStartX + rightMargin, sliderY, sliderWidth, 20, Text.literal("X: "), Text.literal(" %"), -200, 200, entry.getXOffset());
        this.addDrawableChild(this.xSlider);

        sliderY += 20 + spacing;
        this.ySlider = new YSlider(rightStartX + rightMargin, sliderY, sliderWidth, 20, Text.literal("Y: "), Text.literal(" %"), -200, 200, entry.getYOffset());
        this.addDrawableChild(this.ySlider);

        sliderY += 20 + spacing;
        this.scaleSlider = new ScaleSlider(rightStartX + rightMargin, sliderY, sliderWidth, 20, Text.translatable("overlayer.screen.image_edit.slide.zoom"), Text.literal(""), 1, 300, (int) (entry.getScale() * 100));
        this.addDrawableChild(this.scaleSlider);

        sliderY += 20 + spacing;
        this.alphaSlider = new AlphaSlider(rightStartX + rightMargin, sliderY, sliderWidth, 20, Text.translatable("overlayer.screen.image_edit.slide.alpha"), Text.literal(""), 1, 100, (int) (entry.getAlpha() * 100));
        this.addDrawableChild(this.alphaSlider);

        // 3) 图层标签
        sliderY += 20 + spacing + 4;
        int rowWidth = sliderWidth;
        TextWidget layerLabel = new TextWidget(Text.translatable("overlayer.screen.image_edit.label.layer"), this.textRenderer);
        layerLabel.setX(rightStartX + rightMargin);
        layerLabel.setY(sliderY + 2);
        this.addDrawableChild(layerLabel);

        // 4) 图层输入框
        sliderY += 20 + spacing;
        this.layerInput = new TextFieldWidget(this.textRenderer, rightStartX + rightMargin, sliderY, rowWidth, 20, Text.literal("图层"));
        this.layerInput.setText(String.valueOf(entry.getLayer()));
        // 修正: setFilter 改为 setTextPredicate
        this.layerInput.setTextPredicate(s -> s.matches("\\d*"));
        this.layerInput.setMaxLength(6);
        this.layerInput.setChangedListener(s -> {
            try {
                int newLayer = Integer.parseInt(s.trim());
                if (newLayer != entry.getLayer()) {
                    isChanged = true;
                }
            } catch (NumberFormatException ignored) {
                isChanged = true;
            }
        });
        this.addDrawableChild(this.layerInput);

        // 5) 模式按钮
        sliderY += 20 + spacing;
        this.modeButton = ButtonWidget.builder(
                Text.translatable("overlayer.screen.image_edit.button.mode").append(MODES[modeIndex]), (btn) -> this.cycleMode()
        ).position(rightStartX + rightMargin, sliderY).size(rowWidth, 20).build();
        this.addDrawableChild(this.modeButton);

        // 6) 底部按钮
        int buttonY = screenHeight - 30;
        int btnWidth = Math.min(100, (rightWidth - 20) / 2);
        int btnSpacing = 10;
        int totalBtnWidth = btnWidth * 2 + btnSpacing;
        int btnStartX = rightStartX + (rightWidth - totalBtnWidth) / 2;

        this.saveButton = ButtonWidget.builder(SAVE, (btn) -> this.saveAndClose()).position(btnStartX, buttonY).size(btnWidth, 20).build();
        this.addDrawableChild(this.saveButton);

        this.cancelButton = ButtonWidget.builder(CANCEL, (btn) -> this.cancel()).position(btnStartX + btnWidth + btnSpacing, buttonY).size(btnWidth, 20).build();
        this.addDrawableChild(this.cancelButton);

        loadPreviewDimension(entry.getPath());
        isChanged = false;
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

            context.drawTexture(RenderLayer::getGuiTextured, texture, drawX, drawY, 0.0f, 0.0f, drawWidth, drawHeight, drawWidth, drawHeight);
        } else {
            context.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF888888);
            String message;
            if (currentPreviewPath != null && !previewFileExists) {
                message = Text.translatable("overlayer.toast.warning.invalid_path.meg.no_file").getString();
            } else {
                message = Text.translatable("overlayer.screen.image_edit.label.preview").getString();
            }
            // 修正: getWidth 方法
            context.drawText(this.textRenderer, message, previewX + previewSize / 2 - this.textRenderer.getWidth(message) / 2, previewY + previewSize / 2 - 4, 0xFFFFFF, false);
        }
    }

    // ========== 交互事件 ==========

    private void cycleMode() {
        modeIndex = (modeIndex + 1) % MODES.length;
        this.modeButton.setMessage(Text.translatable("overlayer.screen.image_edit.button.mode").append(MODES[modeIndex]));
        entry.setDisplayMode(MODE_VALUES[modeIndex]);
        isChanged = true;
    }

    private void openFileChooser() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            String result = selectFile(stack);
            if (result != null) {
                this.pathInput.setText(result);
                entry.setPath(result);
                entry.clearCache();
                loadPreviewDimension(result);
                isChanged = true;
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
        isChanged = false;

        if (this.client != null) {
            this.client.setScreen(lastScreen);
        }
    }

    private void cancel() {
        if (this.client == null) return;
        if (isChanged) {
            this.client.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            this.client.setScreen(lastScreen);
                        }
                    }, Text.translatable("overlayer.screen.unsaved.title"), Text.translatable("overlayer.screen.unsaved.meg"), ScreenTexts.YES, ScreenTexts.NO
            ));
        } else {
            this.client.setScreen(lastScreen);
        }
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

    // ========== 自定义滑块内部类（使用原版 SliderWidget） ==========
    private abstract class AbstractCustomSlider extends SliderWidget {
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
            this.format = new DecimalFormat("0.00");
            // 计算初始 value (0~1)
            this.value = (currentValue - min) / (double) (max - min);
            updateMessage();
        }

        @Override
        protected abstract void applyValue();

        @Override
        protected void updateMessage() {
            double val = getValue();
            setMessage(Text.literal(prefix.getString() + format.format(val) + suffix.getString()));
        }

        // 改为 public 以便外部调用
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
            isChanged = true;
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
            isChanged = true;
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
            isChanged = true;
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
            isChanged = true;
        }

        @Override
        protected void updateMessage() {
            double val = getValue() / 100.0;
            setMessage(Text.translatable("overlayer.screen.image_edit.slide.alpha").append(df.format(val)));
        }
    }
}
