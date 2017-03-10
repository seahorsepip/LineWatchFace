package com.seapip.thomas.line_watchface;

public enum  BackgroundEffect {
    NONE(0),
    DARKEN(1),
    BLUR(2),
    DARKEN_BLUR(3);

    private int value;

    BackgroundEffect(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static BackgroundEffect fromValue(int value) {
        for (BackgroundEffect backgroundEffect : BackgroundEffect.values()) {
            if (backgroundEffect.getValue() == value) {
                return backgroundEffect;
            }
        }
        return null;
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
