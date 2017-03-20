package com.seapip.thomas.line_watchface;

public enum BackgroundEffectPreference implements Preference {
    NONE(0),
    DARKEN(1),
    BLUR(2),
    DARKEN_BLUR(3);

    private int value;

    BackgroundEffectPreference(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public BackgroundEffectPreference fromValue(int value) {
        for (BackgroundEffectPreference backgroundEffectPreference : BackgroundEffectPreference.values()) {
            if (backgroundEffectPreference.getValue() == value) {
                return backgroundEffectPreference;
            }
        }
        return NONE;
    }

    @Override
    public String toString() {
        switch (this){
            default:
            case NONE:
                return "None";
            case DARKEN:
                return "Darken";
            case BLUR:
                return "Blur";
            case DARKEN_BLUR:
                return "Darken & blur";
        }
    }
}
