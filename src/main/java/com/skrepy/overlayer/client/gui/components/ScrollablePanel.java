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
    private final List<GuiEventListener> children = new ArrayList<>();
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
        children.add(widget);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(getX(), getY() - scrollOffset);

        int localX = mouseX - getX();
        int localY = mouseY - getY() + scrollOffset;

        for (GuiEventListener child : children) {
            if (child instanceof AbstractWidget widget) {
                widget.extractRenderState(guiGraphics, localX, localY, partialTick);
            } else if (child instanceof Renderable renderable) {
                renderable.extractRenderState(guiGraphics, localX, localY, partialTick);
            }
        }

        guiGraphics.pose().popMatrix();
        guiGraphics.disableScissor();

        if (contentHeight > getHeight()) {
            int barHeight = (int) ((float) getHeight() / contentHeight * getHeight());
            int barY = getY() + (int) ((float) scrollOffset / (contentHeight - getHeight()) * (getHeight() - barHeight));
            guiGraphics.fill(getX() + getWidth() - 6, barY, getX() + getWidth() - 2, barY + barHeight, 0xAAFFFFFF);
        }
    }

    // ----- 鼠标事件 -----
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;
        double localX = event.x() - getX();
        double localY = event.y() - getY() + scrollOffset;
        MouseButtonEvent localEvent = new MouseButtonEvent(localX, localY, event.buttonInfo());
        for (GuiEventListener child : children) {
            if (child.isMouseOver(localX, localY)) {
                if (child.mouseClicked(localEvent, doubleClick)) {
                    this.setFocused(child);
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
                double localX = event.x() - getX();
                double localY = event.y() - getY() + scrollOffset;
                MouseButtonEvent localEvent = new MouseButtonEvent(localX, localY, event.buttonInfo());
                return this.focusedChild.mouseReleased(localEvent);
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
        if (this.dragging && this.focusedChild != null) {
            double localX = event.x() - getX();
            double localY = event.y() - getY() + scrollOffset;
            MouseButtonEvent localEvent = new MouseButtonEvent(localX, localY, event.buttonInfo());
            return this.focusedChild.mouseDragged(localEvent, dx, dy);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        int maxScroll = Math.max(0, contentHeight - getHeight());
        if (maxScroll <= 0) return false;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY * 15));
        return true;
    }

    // ----- 键盘事件 -----
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
        return children;
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
}
