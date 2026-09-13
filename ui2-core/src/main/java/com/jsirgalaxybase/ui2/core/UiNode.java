package com.jsirgalaxybase.ui2.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.jsirgalaxybase.ui2.geometry.UiRect;

public final class UiNode {
    private static final AtomicLong IDS = new AtomicLong();
    private final long instanceId = IDS.incrementAndGet();
    private UiElement element;
    private final List<UiNode> children = new ArrayList<UiNode>();
    private final Map<String, Object> localState = new HashMap<String, Object>();
    private UiRect bounds = new UiRect(0, 0, 0, 0);
    private boolean mounted = true;

    UiNode(UiElement element) { this.element = element; }
    public long getInstanceId() { return instanceId; }
    public UiElement getElement() { return element; }
    void setElement(UiElement element) { this.element = element; }
    public List<UiNode> getChildren() { return children; }
    public Map<String, Object> getLocalState() { return localState; }
    public UiRect getBounds() { return bounds; }
    public void setBounds(UiRect bounds) { this.bounds = bounds; }
    public boolean isMounted() { return mounted; }
    public void unmount() { mounted = false; for (UiNode child : children) child.unmount(); localState.clear(); }
}
