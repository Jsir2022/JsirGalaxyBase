package com.jsirgalaxybase.client.ui2;

import org.lwjgl.input.Keyboard;

import com.jsirgalaxybase.ui2.input.UiKeyCode;

public final class MinecraftKeyMapper {
    private MinecraftKeyMapper() {}
    public static UiKeyCode map(int keyCode) {
        switch (keyCode) {
            case Keyboard.KEY_ESCAPE: return UiKeyCode.ESCAPE;
            case Keyboard.KEY_TAB: return UiKeyCode.TAB;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER: return UiKeyCode.ENTER;
            case Keyboard.KEY_SPACE: return UiKeyCode.SPACE;
            case Keyboard.KEY_LEFT: return UiKeyCode.LEFT;
            case Keyboard.KEY_RIGHT: return UiKeyCode.RIGHT;
            case Keyboard.KEY_UP: return UiKeyCode.UP;
            case Keyboard.KEY_DOWN: return UiKeyCode.DOWN;
            case Keyboard.KEY_BACK: return UiKeyCode.BACKSPACE;
            case Keyboard.KEY_DELETE: return UiKeyCode.DELETE;
            case Keyboard.KEY_HOME: return UiKeyCode.HOME;
            case Keyboard.KEY_END: return UiKeyCode.END;
            default: return UiKeyCode.UNKNOWN;
        }
    }
}
