package com.jsirgalaxybase.ui2.lab;

public enum UiLabPage {
    OVERVIEW("概览"), CONTROLS("控件"), DATA("数据"), OVERLAY("浮层"), INVENTORY("库存");

    private final String label;
    UiLabPage(String label) { this.label = label; }
    public String getLabel() { return label; }
}
