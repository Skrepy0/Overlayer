package com.skrepy.overlayer.client.gui;

import com.skrepy.overlayer.Config;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    private static final Text[] LABELS = {Text.translatable("overlayer.configuration.titleScreenBtnXOffset"), Text.translatable("overlayer.configuration.titleScreenBtnYOffset"), Text.translatable("overlayer.configuration.optionsScreenBtnXOffset"), Text.translatable("overlayer.configuration.optionsScreenBtnYOffset")
    };

    private final TextFieldWidget[] inputFields = new TextFieldWidget[4];

    public ConfigScreen() {
        super(Text.translatable("overlayer.configuration"));
    }

    private int getOptionsValueFromConfig(int index) {
        return switch (index) {
            case 0 -> Config.getTitleScreenBtnXOffset();
            case 1 -> Config.getTitleScreenBtnYOffset();
            case 2 -> Config.getOptionsScreenBtnXOffset();
            case 3 -> Config.getOptionsScreenBtnYOffset();
            default -> 0;
        };
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        // 标题
        TextWidget titleWidget = new TextWidget(this.title, this.textRenderer);
        titleWidget.setX(centerX - this.textRenderer.getWidth(this.title) / 2);
        titleWidget.setY(20);
        addDrawableChild(titleWidget);

        int titleBottom = titleWidget.getY() + this.textRenderer.fontHeight + 20;

        int startY = titleBottom + 10;
        int fieldWidth = 120;
        int fieldHeight = 20;
        int spacing = 10;          // 行间距
        int labelFieldGap = 64;    // 标签与输入框之间的间距

        int maxLabelWidth = 0;
        for (Text labelText : LABELS) {
            int w = this.textRenderer.getWidth(labelText);
            if (w > maxLabelWidth) maxLabelWidth = w;
        }

        int totalWidth = maxLabelWidth + labelFieldGap + fieldWidth;
        int startX = centerX - totalWidth / 2; // 整体居中

        for (int i = 0; i < 4; i++) {
            int y = startY + i * (fieldHeight + spacing + 12);

            TextWidget label = new TextWidget(LABELS[i], this.textRenderer);
            label.setX(startX);
            label.setY(y + (fieldHeight - this.textRenderer.fontHeight) / 2);
            addDrawableChild(label);

            TextFieldWidget textField = new TextFieldWidget(
                    this.textRenderer, startX + maxLabelWidth + labelFieldGap, y, fieldWidth, fieldHeight, Text.literal("")
            );
            textField.setTextPredicate(s -> s.matches("-?\\d*"));
            textField.setMaxLength(11);
            textField.setText(Integer.toString(getOptionsValueFromConfig(i)));
            inputFields[i] = textField;
            addDrawableChild(textField);
        }

        int buttonY = startY + 5 * (fieldHeight + spacing + 12) + 20;
        ButtonWidget doneButton = ButtonWidget.builder(
                ScreenTexts.DONE, (btn) -> this.close()
        ).position(centerX - 130, buttonY).size(260, 20).build();
        addDrawableChild(doneButton);
    }

    @Override
    public void close() {
        Config.setTitleScreenBtnXOffset(getValue(0));
        Config.setTitleScreenBtnYOffset(getValue(1));
        Config.setOptionsScreenBtnXOffset(getValue(2));
        Config.setOptionsScreenBtnYOffset(getValue(3));
        OverlayerManager.getInstance().save();
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    public int getValue(int index) {
        if (index < 0 || index >= inputFields.length) return 0;
        try {
            return Integer.parseInt(inputFields[index].getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setValue(int index, int value) {
        if (index >= 0 && index < inputFields.length) {
            inputFields[index].setText(Integer.toString(value));
        }
    }
}
