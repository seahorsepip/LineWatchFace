package com.seapip.thomas.line_watchface;

public enum NotificationIndicatorPreference implements Preference {
    DISABLED(0),
    UNREAD(1),
    ALL(2);

    private int value;

    NotificationIndicatorPreference(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public NotificationIndicatorPreference fromValue(int value) {
        for (NotificationIndicatorPreference notificationIndicatorPreference : NotificationIndicatorPreference.values()) {
            if (notificationIndicatorPreference.getValue() == value) {
                return notificationIndicatorPreference;
            }
        }
        return DISABLED;
    }

    @Override
    public String toString() {
        switch (this){
            default:
            case DISABLED:
                return "Disabled";
            case UNREAD:
                return "Unread notifications";
            case ALL:
                return "All notifications";
        }
    }
}
