package com.seapip.thomas.line_watchface;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class ConfigItem {

    static final public int PREFERENCE = 1;
    static final public int COMPLICATION = 2;

    private String title;
    private Intent activity;
    private Drawable image;
    private String value;
    private int requestCode;

    private String preference;
    private Preference preferenceValue;
    private int complicationId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Drawable getImage() {
        return image;
    }

    public void setImage(Drawable image) {
        this.image = image;
    }

    public Intent getActivity() {
        return activity;
    }

    public void setActivity(Intent activity) {
        this.activity = activity;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public Preference getPreferenceValue() {
        return preferenceValue;
    }

    public void setPreferenceValue(Preference preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public int getComplicationId() {
        return complicationId;
    }

    public void setComplicationId(int complicationId) {
        this.complicationId = complicationId;
    }

    public ConfigItem(String title, Intent activity, Drawable image, String value) {
        this.title = title;
        this.activity = activity;
        this.image = image;
        this.value = value;
    }

    public ConfigItem(String title, Intent activity, Drawable image, String value, String preference, Preference preferenceValue) {
        this(title, activity, image, value);
        this.requestCode = PREFERENCE;
        this.preference = preference;
        this.preferenceValue = preferenceValue;
    }

    public ConfigItem(String title, Intent activity, Drawable image, String value, int complicationId) {
        this(title, activity, image, value);
        this.requestCode = COMPLICATION;
        this.complicationId = complicationId;
    }
}