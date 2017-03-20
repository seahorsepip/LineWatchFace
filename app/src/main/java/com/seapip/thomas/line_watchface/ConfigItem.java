package com.seapip.thomas.line_watchface;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class ConfigItem {

    private String title;
    private Intent activity;
    private Drawable image;
    private String text;

    public ConfigItem(String title, Intent activity, Drawable image, String text) {
        this.title = title;
        this.activity = activity;
        this.image = image;
        this.text = text;
    }

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}