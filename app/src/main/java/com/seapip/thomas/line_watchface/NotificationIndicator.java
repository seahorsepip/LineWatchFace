package com.seapip.thomas.line_watchface;

public enum NotificationIndicator {
    DISABLED(0),
    UNREAD(1),
    ALL(2);

    private int value;

    NotificationIndicator(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static NotificationIndicator fromValue(int value) {
        for (NotificationIndicator notificationIndicator : NotificationIndicator.values()) {
            if (notificationIndicator.getValue() == value) {
                return notificationIndicator;
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
