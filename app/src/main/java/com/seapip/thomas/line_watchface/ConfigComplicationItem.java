package com.seapip.thomas.line_watchface;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.wearable.complications.ComplicationHelperActivity;

public class ConfigComplicationItem extends ConfigItem {

    private int id;

    public ConfigComplicationItem(String title, Drawable image, String value, Context context,
                                  ComponentName watchFace, int id,
                                  int[] complicationSupportedTypes) {
        super(title,
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        context,
                        watchFace,
                        id,
                        complicationSupportedTypes),
                image,
                value);
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
