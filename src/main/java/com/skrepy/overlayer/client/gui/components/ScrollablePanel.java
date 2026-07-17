package com.skrepy.overlayer.client.gui.components;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class ScrollablePanel extends ClickableWidget {
    private final List<ChildEntry> children = new ArrayList<>();
    private final int contentHeight;
    private int scrollOffset = 0;
    private Element focusedChild = null;
    private boolean dragging = false;

    public ScrollablePanel(int x, int y, int width, int height, int contentHeight) {
        super(x, y, width, height, Text.empty());
        this.contentHeight = contentHeight;
    }

    public void addWidget(Element widget) {
        if (widget instanceof ClickableWidget aw) {
            int relX = aw.getX();
            int relY = aw.getY();
            children.add(new ChildEntry(widget, relX, relY));
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
    public void renderButton(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        updateAllPositions();

        for (ChildEntry entry : children) {
            if (entry.widget instanceof ClickableWidget w) {
                int top = w.getY();
                int bottom = w.getY() + w.getHeight();
                if (bottom > getY() && top < getY() + getHeight()) {
                    w.render(context, mouseX, mouseY, deltaTicks);
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

    // ---- 鼠标事件 ----
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {

            return false;
        }

        boolean anyChildHandled = false;
        for (ChildEntry entry : children) {
            Element child = entry.widget;
            boolean over = child.isMouseOver(mouseX, mouseY);
            if (over) {
                boolean handled = child.mouseClicked(mouseX, mouseY, button);
                if (handled) {
                    setFocusedChild(child);
                    if (button == 0) dragging = true;
                    return true;
                }
            }
        }

        if (!anyChildHandled) {
            setFocusedChild(null);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            if (focusedChild != null) {
                return focusedChild.mouseReleased(mouseX, mouseY, button);
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && focusedChild != null) {
            return focusedChild.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (focusedChild != null && focusedChild.isMouseOver(mouseX, mouseY)) {
            return focusedChild.mouseScrolled(mouseX, mouseY, amount);
        }
        int maxScroll = Math.max(0, contentHeight - getHeight());
        if (maxScroll == 0) return false;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - amount * 15));
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
    public boolean charTyped(char chr, int modifiers) {
        return focusedChild != null && focusedChild.charTyped(chr, modifiers);
    }

    // ---- 焦点管理 ----
    public void setFocusedChild(Element child) {
        if (focusedChild != null && focusedChild != child) {
            focusedChild.setFocused(false);
        }
        focusedChild = child;
        if (child != null) {
            child.setFocused(true);
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
    }

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
    }

    public void clearChildren() {
        this.children.clear();
        this.focusedChild = null;
        this.dragging = false;
    }

    private record ChildEntry(Element widget, int relX, int relY) {
        void updatePosition(int panelX, int panelY, int scrollOffset) {
            if (widget instanceof ClickableWidget w) {
                w.setX(panelX + relX);
                w.setY(panelY + relY - scrollOffset);
            }
        }
    }
}
