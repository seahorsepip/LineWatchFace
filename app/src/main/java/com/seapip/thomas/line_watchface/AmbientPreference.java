package com.seapip.thomas.line_watchface;

public enum AmbientPreference implements Preference {
    GRAYSCALE(0),
    COLOR(1);

    private int value;

    AmbientPreference(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public AmbientPreference fromValue(int value) {
        for (AmbientPreference ambientPreference : AmbientPreference.values()) {
            if (ambientPreference.getValue() == value) {
                return ambientPreference;
            }
        }
        return GRAYSCALE;
    }

    @Override
    public String toString() {
        switch (this){
            default:
            case GRAYSCALE:
                return "Grayscale";
            case COLOR:
                return "Color";
        }
    }
}
