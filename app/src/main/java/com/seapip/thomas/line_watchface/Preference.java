package com.seapip.thomas.line_watchface;

public interface Preference {

    int getValue();

    Preference fromValue(int value);

    String toString();
}
