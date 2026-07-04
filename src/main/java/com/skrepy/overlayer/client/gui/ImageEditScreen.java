package com.skrepy.overlayer.client.gui;

import java.text.DecimalFormat;
import java.util.function.Consumer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.skrepy.overlayer.Overlayer;
import com.skrepy.overlayer.data.ImageEntry;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

@OnlyIn(Dist.CLIENT)
public class ImageEditScreen extends Screen {
    private static final Component TITLE = Component.literal("编辑图片实例");
    private static final Component SAVE = Component.literal("保存");
    private static final Component CANCEL = Component.literal("取消");

    private final Screen lastScreen;
    private final ImageEntry entry;
    private final Consumer<ImageEntry> onSave;

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
    private static final String[] MODES = {"总是", "仅游戏中", "仅非游戏中", "禁用"};
    private static final String[] MODE_VALUES = {"always", "ingame", "not_ingame", "disabled"};

    private int previewSize = 64;
    private int previewX;
    private int previewY;

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

        int centerX = this.width / 2;
        int startY = 30;

        // 标题
        StringWidget titleWidget = new StringWidget(TITLE, this.font);
        titleWidget.setX(centerX - this.font.width(TITLE) / 2);
        titleWidget.setY(10);
        this.addRenderableWidget(titleWidget);

        // 预览
        previewX = centerX - previewSize / 2;
        previewY = startY;

        // 路径输入
        int inputY = previewY + previewSize + 10;
        this.pathInput = new EditBox(this.font, centerX - 110, inputY, 200, 20, Component.literal("图片路径"));
        this.pathInput.setValue(entry.getPath());
        this.pathInput.setMaxLength(Integer.MAX_VALUE);
        this.addRenderableWidget(this.pathInput);

        this.browseButton = Button.builder(Component.literal("..."), (btn) -> this.openFileChooser()).pos(centerX + 95, inputY).size(20, 20).build();
        this.addRenderableWidget(this.browseButton);

        // --- 滑动条区域 ---
        int sliderY = inputY + 30;
        int sliderWidth = 200;
        int sliderHeight = 20;
        int spacing = 4;

        // X 偏移
        this.xSlider = new XSlider(centerX - sliderWidth / 2, sliderY, sliderWidth, sliderHeight, Component.literal("X: "), Component.literal(" %"), -200, 200, entry.getXOffset(), 1, 0, true);
        this.addRenderableWidget(this.xSlider);

        // Y 偏移
        sliderY += sliderHeight + spacing;
        this.ySlider = new YSlider(centerX - sliderWidth / 2, sliderY, sliderWidth, sliderHeight, Component.literal("Y: "), Component.literal(" %"), -200, 200, entry.getYOffset(), 1, 0, true);
        this.addRenderableWidget(this.ySlider);

        // 缩放
        sliderY += sliderHeight + spacing;
        this.scaleSlider = new ScaleSlider(centerX - sliderWidth / 2, sliderY, sliderWidth, sliderHeight, Component.literal("缩放: "), Component.literal(""), 1, 300, (int) (entry.getScale() * 100), 1, 0, false);
        this.addRenderableWidget(this.scaleSlider);

        // 透明度
        sliderY += sliderHeight + spacing;
        this.alphaSlider = new AlphaSlider(centerX - sliderWidth / 2, sliderY, sliderWidth, sliderHeight, Component.literal("透明度: "), Component.literal(""), 1, 100, (int) (entry.getAlpha() * 100), 1, 0, false);
        this.addRenderableWidget(this.alphaSlider);

        // 图层优先级（数字输入框 + 标签）
        sliderY += sliderHeight + spacing + 4;
        StringWidget layerLabel = new StringWidget(Component.literal("图层:"), this.font);
        layerLabel.setX(centerX - 50 - this.font.width("图层:"));
        layerLabel.setY(sliderY + 2);
        this.addRenderableWidget(layerLabel);

        this.layerInput = new EditBox(this.font, centerX - 30, sliderY, 60, 20, Component.literal("图层"));
        this.layerInput.setValue(String.valueOf(entry.getLayer()));
        this.layerInput.setFilter(s -> s.matches("\\d*"));
        this.layerInput.setMaxLength(6);
        this.addRenderableWidget(this.layerInput);

        // 模式切换按钮
        int modeY = sliderY + 30;
        this.modeButton = Button.builder(
                Component.literal("模式: " + MODES[modeIndex]), (btn) -> this.cycleMode()
        ).pos(centerX - 60, modeY).size(120, 20).build();
        this.addRenderableWidget(this.modeButton);

        // 底部按钮
        int buttonY = this.height - 30;
        this.saveButton = Button.builder(SAVE, (btn) -> this.saveAndClose()).pos(centerX - 110, buttonY).size(100, 20).build();
        this.addRenderableWidget(this.saveButton);

        this.cancelButton = Button.builder(CANCEL, (btn) -> this.cancel()).pos(centerX + 10, buttonY).size(100, 20).build();
        this.addRenderableWidget(this.cancelButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 预览
        var texture = entry.getOriginalTexture(this.minecraft.getTextureManager());
        if (texture != null) {
            // 直接绘制到预览区域，不缩放（或者缩放至 previewSize）
            // 简单拉伸至 previewSize
            guiGraphics.blit(texture, previewX, previewY, 0, 0, previewSize, previewSize, previewSize, previewSize);
        } else {
            guiGraphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF888888);
            guiGraphics.drawString(this.font, "预览", previewX + previewSize / 2 - 10, previewY + previewSize / 2 - 4, 0xFFFFFF);
        }
    }

    private void cycleMode() {
        modeIndex = (modeIndex + 1) % MODES.length;
        this.modeButton.setMessage(Component.literal("模式: " + MODES[modeIndex]));
        // 更新 entry 的模式，但保存时才应用，但预览不需要实时更新模式，因为模式不影响预览（编辑时始终显示）
        entry.setDisplayMode(MODE_VALUES[modeIndex]);
    }

    private void openFileChooser() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filterPatterns = stack.mallocPointer(5);
            filterPatterns.put(stack.UTF8("*.png"));
            filterPatterns.put(stack.UTF8("*.jpg"));
            filterPatterns.put(stack.UTF8("*.jpeg"));
            filterPatterns.put(stack.UTF8("*.bmp"));
            filterPatterns.put(stack.UTF8("*.gif"));
            filterPatterns.flip();

            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    "选择图片文件", null, filterPatterns, null, false
            );
            if (result != null) {
                this.pathInput.setValue(result);
                // 路径变化，清除缓存并更新预览
                entry.setPath(result);
                entry.clearCache();
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

        // 收集滑动条值（已经实时更新到 entry，但为确保）
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

        // 模式已经在 cycleMode 中更新，但可能未保存，确保设置
        entry.setDisplayMode(MODE_VALUES[modeIndex]);
        OverlayerManager.getInstance().save();
        onSave.accept(entry);
        this.minecraft.setScreen(lastScreen);
    }

    private void cancel() {
        this.minecraft.setScreen(lastScreen);
    }

    @Override
    public void onClose() {
        this.cancel();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------- 自定义滑动条内部类 ----------
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
            setMessage(Component.literal("缩放: " + df.format(val) + "x"));
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
            setMessage(Component.literal("透明度: " + df.format(val)));
        }
    }
}
