package com.jsirgalaxybase.ui2.resource;

public interface ResourceResolver {
    boolean canResolve(String resourceId);

    ResourceResolver EMPTY = new ResourceResolver() {
        @Override public boolean canResolve(String resourceId) { return false; }
    };
}
