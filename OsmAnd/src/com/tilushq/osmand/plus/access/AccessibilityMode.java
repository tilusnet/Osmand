package com.tilushq.osmand.plus.access;

import com.tilushq.osmand.plus.ClientContext;
import com.tilushq.osmand.plus.R;

public enum AccessibilityMode {

    ON(R.string.accessibility_on),
    OFF(R.string.accessibility_off),
    DEFAULT(R.string.accessibility_default);

    private final int key;

    AccessibilityMode(int key) {
        this.key = key;
    }

    public String toHumanString(ClientContext ctx) {
        return ctx.getString(key);
    }

}
