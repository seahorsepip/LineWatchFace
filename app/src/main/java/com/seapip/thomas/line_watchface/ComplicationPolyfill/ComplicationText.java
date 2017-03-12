package com.seapip.thomas.line_watchface.ComplicationPolyfill;

import android.content.Context;

public class ComplicationText {
    CharSequence text;

    public CharSequence getText() {
        return text;
    }

    public CharSequence getText(Context context, long CurrentTimeMillis) {
        return getText();
    }

    public ComplicationText(CharSequence text) {
        this.text = text;
    }
}
