package com.skrepy.overlayer.client.gui.components;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

public class ScrollablePanel extends AbstractWidget implements ContainerEventHandler {
    private final List<ChildEntry> children = new ArrayList<>();
    private final int contentHeight;
    private int scrollOffset = 0;
    @Nullable
    private GuiEventListener focusedChild = null;
    private boolean dragging = false;

    public ScrollablePanel(int x, int y, int width, int height, int contentHeight) {
        super(x, y, width, height, Component.empty());
        this.contentHeight = contentHeight;
    }

    public void addWidget(GuiEventListener widget) {
        if (widget instanceof AbstractWidget aw) {
            // 添加时，aw 的坐标是相对于面板内部的
            children.add(new ChildEntry(widget, aw.getX(), aw.getY()));
            updateAllPositions();
        }
    }

    private void updateAllPositions() {
        for (ChildEntry entry : children) {
            entry.updatePosition(getX(), getY(), scrollOffset);
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateAllPositions();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateAllPositions();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());

        updateAllPositions();

        for (ChildEntry entry : children) {
            if (entry.widget instanceof AbstractWidget aw) {
                int top = aw.getY();
                int bottom = aw.getY() + aw.getHeight();
                if (top < getY() + getHeight() && bottom > getY()) {
                    aw.render(graphics, mouseX, mouseY, partialTick);
                }
            } else if (entry.widget instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        graphics.disableScissor();

        if (contentHeight > getHeight()) {
            int barHeight = (int) ((float) getHeight() / contentHeight * getHeight());
            int barY = getY() + (int) ((float) scrollOffset / (contentHeight - getHeight()) * (getHeight() - barHeight));
            graphics.fill(getX() + getWidth() - 6, barY, getX() + getWidth() - 2, barY + barHeight, 0xAAFFFFFF);
        }
    }

    // ---- 鼠标事件（直接传递屏幕坐标） ----
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        for (ChildEntry entry : children) {
            if (entry.widget.isMouseOver(mouseX, mouseY)) {
                if (entry.widget.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(entry.widget);
                    if (button == 0) {
                        this.dragging = true;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.dragging) {
            this.dragging = false;
            if (this.focusedChild != null) {
                return this.focusedChild.mouseReleased(mouseX, mouseY, button);
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging && this.focusedChild != null) {
            return this.focusedChild.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (focusedChild != null && focusedChild.isMouseOver(mouseX, mouseY)) {
            return focusedChild.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int maxScroll = Math.max(0, contentHeight - getHeight());
        if (maxScroll <= 0) return false;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * 15));
        updateAllPositions();
        return true;
    }

    // ---- 键盘事件 ----
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return focusedChild != null && focusedChild.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return focusedChild != null && focusedChild.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return focusedChild != null && focusedChild.charTyped(codePoint, modifiers);
    }

    // ----- 焦点管理 -----
    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return children.stream().map(e -> e.widget).toList();
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return focusedChild;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener child) {
        if (this.focusedChild != null && this.focusedChild != child) {
            this.focusedChild.setFocused(false);
        }
        this.focusedChild = child;
        if (child != null) {
            child.setFocused(true);
        }
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= getY() && mouseY <= getY() + getHeight();
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(getX(), getY(), getWidth(), getHeight());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal("scrollable panel"));
    }

    private record ChildEntry(GuiEventListener widget, int relX, int relY) {

        void updatePosition(int panelX, int panelY, int scrollOffset) {
            if (widget instanceof AbstractWidget aw) {
                aw.setX(panelX + relX);
                aw.setY(panelY + relY - scrollOffset);
            }
        }
    }
}
