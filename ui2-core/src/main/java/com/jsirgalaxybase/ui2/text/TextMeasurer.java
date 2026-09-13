package com.jsirgalaxybase.ui2.text;

import com.jsirgalaxybase.ui2.geometry.UiSize;

public interface TextMeasurer {
    UiSize measure(String text, TextStyle style, int maxWidth);
}
