package com.skrepy.overlayer.client.gui.components;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * 可滚动面板 - 仅依赖 Element 接口，无需 Container 或 ContainerEventHandler。
 * 适用于 Fabric 1.21.5（Mojang映射）。
 */
public class ScrollablePanel extends ClickableWidget implements Element {
    private final List<ChildEntry> children = new ArrayList<>();
    private final int contentHeight;
    private int scrollOffset = 0;
    @Nullable
    private Element focusedChild = null;
    private boolean dragging = false;

    public ScrollablePanel(int x, int y, int width, int height, int contentHeight) {
        super(x, y, width, height, Text.empty());
        this.contentHeight = contentHeight;
    }

    public void addWidget(Element widget) {
        if (widget instanceof ClickableWidget aw) {
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
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());

        updateAllPositions();

        for (ChildEntry entry : children) {
            if (entry.widget instanceof ClickableWidget widget) {
                int top = widget.getY();
                int bottom = widget.getY() + widget.getHeight();
                if (top < getY() + getHeight() && bottom > getY()) {
                    widget.render(context, mouseX, mouseY, deltaTicks);
                }
            }
        }

        context.disableScissor();

        if (contentHeight > getHeight()) {
            int barHeight = (int) ((float) getHeight() / contentHeight * getHeight());
            int barY = getY() + (int) ((float) scrollOffset / (contentHeight - getHeight()) * (getHeight() - barHeight));
            context.fill(getX() + getWidth() - 6, barY, getX() + getWidth() - 2, barY + barHeight, 0xAAFFFFFF);
        }
    }

    // ---- 鼠标事件（手动转发给子控件） ----
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        for (ChildEntry entry : children) {
            if (entry.widget.isMouseOver(mouseX, mouseY)) {
                if (entry.widget.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(entry.widget);
                    if (button == 0) this.dragging = true;
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.dragging && this.focusedChild != null) {
            return this.focusedChild.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (focusedChild != null && focusedChild.isMouseOver(mouseX, mouseY)) {
            return focusedChild.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int maxScroll = Math.max(0, contentHeight - getHeight());
        if (maxScroll <= 0) return false;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 15));
        updateAllPositions();
        return true;
    }

    // ---- 键盘事件（转发给焦点子控件） ----
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return focusedChild != null && focusedChild.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return focusedChild != null && focusedChild.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return focusedChild != null && focusedChild.charTyped(chr, modifiers);
    }

    public List<? extends Element> children() {
        return children.stream().map(e -> e.widget).toList();
    }

    @Nullable
    public Element getFocused() {
        return focusedChild;
    }

    // ---- 焦点管理（手动实现） ----
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused && this.focusedChild != null) {
            this.focusedChild.setFocused(false);
            this.focusedChild = null;
        }
    }

    public void setFocused(@Nullable Element child) {
        if (this.focusedChild != null && this.focusedChild != child) {
            this.focusedChild.setFocused(false);
        }
        this.focusedChild = child;
        if (child != null) {
            child.setFocused(true);
        }
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    // ---- 辅助 ----
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= getY() && mouseY <= getY() + getHeight();
    }

    @Override
    public SelectionType getType() {
        if (isFocused()) return SelectionType.FOCUSED;
        if (isHovered()) return SelectionType.HOVERED;
        return SelectionType.NONE;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // 可添加叙述
    }

    private record ChildEntry(Element widget, int relX, int relY) {
        void updatePosition(int panelX, int panelY, int scrollOffset) {
            if (widget instanceof ClickableWidget aw) {
                aw.setX(panelX + relX);
                aw.setY(panelY + relY - scrollOffset);
            }
        }
    }
}
