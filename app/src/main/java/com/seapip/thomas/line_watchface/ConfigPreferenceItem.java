package com.seapip.thomas.line_watchface;

import android.graphics.drawable.Drawable;

public class ConfigPreferenceItem extends ConfigItem {

    private String key;
    private Preference value;

    public ConfigPreferenceItem(String title, Drawable image, String key, Preference value) {
        super(title, null, image, value.toString());
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Preference getValue() {
        return value;
    }
}
