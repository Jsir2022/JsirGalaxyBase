package com.jsirgalaxybase.client.gui.framework;

public class GuiRect {

    private int x;
    private int y;
    private int width;
    private int height;

    public GuiRect(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRight() {
        return x + width;
    }

    public int getBottom() {
        return y + height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < getRight() && mouseY >= y && mouseY < getBottom();
    }
}