package com.skrepy.overlayer.client.gui.components;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());

        updateAllPositions();

        for (ChildEntry entry : children) {
            if (entry.widget instanceof AbstractWidget aw) {
                int top = aw.getY();
                int bottom = aw.getY() + aw.getHeight();
                if (top < getY() + getHeight() && bottom > getY()) {
                    aw.extractRenderState(graphics, mouseX, mouseY, partialTick);
                }
            } else if (entry.widget instanceof Renderable renderable) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;
        for (ChildEntry entry : children) {
            if (entry.widget.isMouseOver(event.x(), event.y())) {
                if (entry.widget.mouseClicked(event, doubleClick)) {
                    this.setFocused(entry.widget);
                    if (event.button() == 0) {
                        this.dragging = true;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.dragging) {
            this.dragging = false;
            if (this.focusedChild != null) {
                return this.focusedChild.mouseReleased(event);
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
        if (this.dragging && this.focusedChild != null) {
            return this.focusedChild.mouseDragged(event, dx, dy);
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
    public boolean keyPressed(@NotNull KeyEvent event) {
        return focusedChild != null && focusedChild.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NotNull KeyEvent event) {
        return focusedChild != null && focusedChild.keyReleased(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        return focusedChild != null && focusedChild.charTyped(event);
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
