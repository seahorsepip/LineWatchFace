package com.seapip.thomas.line_watchface.ComplicationPolyfill;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;

public class ComplicationData {
    private static final String TAG = "ComplicationData";
    public static final int TYPE_NOT_CONFIGURED = 1;
    public static final int TYPE_EMPTY = 2;
    public static final int TYPE_NO_DATA = 10;
    public static final int TYPE_SHORT_TEXT = 3;
    public static final int TYPE_LONG_TEXT = 4;
    public static final int TYPE_RANGED_VALUE = 5;
    public static final int TYPE_ICON = 6;
    public static final int TYPE_SMALL_IMAGE = 7;
    public static final int TYPE_LARGE_IMAGE = 8;
    public static final int TYPE_NO_PERMISSION = 9;
    public static final int IMAGE_STYLE_PHOTO = 1;
    public static final int IMAGE_STYLE_ICON = 2;
    private static final String FIELD_START_TIME = "START_TIME";
    private static final String FIELD_END_TIME = "END_TIME";
    private static final String FIELD_SHORT_TITLE = "SHORT_TITLE";
    private static final String FIELD_SHORT_TEXT = "SHORT_TEXT";
    private static final String FIELD_LONG_TITLE = "LONG_TITLE";
    private static final String FIELD_LONG_TEXT = "LONG_TEXT";
    private static final String FIELD_VALUE = "VALUE";
    private static final String FIELD_MIN_VALUE = "MIN_VALUE";
    private static final String FIELD_MAX_VALUE = "MAX_VALUE";
    private static final String FIELD_ICON = "ICON";
    private static final String FIELD_ICON_BURN_IN_PROTECTION = "ICON_BURN_IN_PROTECTION";
    private static final String FIELD_SMALL_IMAGE = "SMALL_IMAGE";
    private static final String FIELD_LARGE_IMAGE = "LARGE_IMAGE";
    private static final String FIELD_TAP_ACTION = "TAP_ACTION";
    private static final String FIELD_IMAGE_STYLE = "IMAGE_STYLE";
    private static final String[][] REQUIRED_FIELDS = new String[][]{null, new String[0], new String[0], {"SHORT_TEXT"}, {"LONG_TEXT"}, {"VALUE", "MIN_VALUE", "MAX_VALUE"}, {"ICON"}, {"SMALL_IMAGE", "IMAGE_STYLE"}, {"LARGE_IMAGE"}, new String[0], new String[0]};
    private static final String[][] OPTIONAL_FIELDS = new String[][]{null, new String[0], new String[0], {"SHORT_TITLE", "ICON", "ICON_BURN_IN_PROTECTION", "TAP_ACTION"}, {"LONG_TITLE", "ICON", "ICON_BURN_IN_PROTECTION", "SMALL_IMAGE", "IMAGE_STYLE", "TAP_ACTION"}, {"SHORT_TEXT", "SHORT_TITLE", "ICON", "ICON_BURN_IN_PROTECTION", "TAP_ACTION"}, {"TAP_ACTION", "ICON_BURN_IN_PROTECTION"}, {"TAP_ACTION"}, {"TAP_ACTION"}, {"SHORT_TEXT", "SHORT_TITLE", "ICON", "ICON_BURN_IN_PROTECTION"}, new String[0]};

    int type;
    int imageStyle;
    ComplicationText shortText;
    ComplicationText longText;
    ComplicationText shortTitle;
    ComplicationText longTitle;
    Icon icon;
    Icon burnInProtectionIcon;
    Icon smallImage;
    Icon largeImage;
    float value;
    float minValue;
    float maxValue;
    PendingIntent tapAction;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getImageStyle() {
        return imageStyle;
    }

    public void setImageStyle(int imageStyle) {
        this.imageStyle = imageStyle;
    }

    public ComplicationText getShortText() {
        return shortText;
    }

    public void setShortText(ComplicationText shortText) {
        this.shortText = shortText;
    }

    public ComplicationText getLongText() {
        return longText;
    }

    public void setLongText(ComplicationText longText) {
        this.longText = longText;
    }

    public ComplicationText getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(ComplicationText shortTitle) {
        this.shortTitle = shortTitle;
    }

    public ComplicationText getLongTitle() {
        return longTitle;
    }

    public void setLongTitle(ComplicationText longTitle) {
        this.longTitle = longTitle;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public Icon getBurnInProtectionIcon() {
        return burnInProtectionIcon;
    }

    public void setBurnInProtectionIcon(Icon burnInProtectionIcon) {
        this.burnInProtectionIcon = burnInProtectionIcon;
    }

    public Icon getSmallImage() {
        return smallImage;
    }

    public void setSmallImage(Icon smallImage) {
        this.smallImage = smallImage;
    }

    public Icon getLargeImage() {
        return largeImage;
    }

    public void setLargeImage(Icon largeImage) {
        this.largeImage = largeImage;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public float getMinValue() {
        return minValue;
    }

    public void setMinValue(float minValue) {
        this.minValue = minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
    }

    public PendingIntent getTapAction() {
        return tapAction;
    }

    public void setTapAction(PendingIntent tapAction) {
        this.tapAction = tapAction;
    }

    public boolean isActive(long currentTimeMillis) {
        return true;
    }
}
