package com.seapip.thomas.line_watchface;

import android.content.Intent;
import android.graphics.drawable.Drawable;

public class ConfigurationItemModel {

    public String title;
    public Intent activity;
    public Drawable image;

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

    public ConfigurationItemModel(String title, Drawable image, Intent activity) {
        this.title = title;
        this.image = image;
        this.activity = activity;
    }
}