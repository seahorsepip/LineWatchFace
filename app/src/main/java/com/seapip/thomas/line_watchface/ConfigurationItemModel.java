package com.seapip.thomas.line_watchface;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class ConfigurationItemModel {

    private String title;
    private Intent activity;
    private Integer requestCode;
    private Drawable image;
    private String value;

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

    public ConfigurationItemModel(String title, Drawable image, Intent activity) {
        this.title = title;
        this.activity = activity;
        this.image = image;
    }

    public ConfigurationItemModel(String title, Drawable image, Intent activity, String value) {
        this(title, image, activity);
        this.value = value;
    }
}