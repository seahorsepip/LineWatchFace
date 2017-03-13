package com.seapip.thomas.line_watchface.complications;

import android.util.Log;

import java.util.Locale;

public class ComplicationProviderService {
    private static final String TAG = "RandomNumberProvider";

    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    public void onComplicationActivated(
            int complicationId, int dataType, ComplicationManager complicationManager) {
    }

    /*
     * Called when the complication needs updated data from your provider. There are four scenarios
     * when this will happen:
     *
     *   1. An active watch face complication is changed to use this provider
     *   2. A complication using this provider becomes active
     *   3. The period of time you specified in the manifest has elapsed (UPDATE_PERIOD_SECONDS)
     *   4. You triggered an update from your own class via the
     *       ProviderUpdateRequester.requestUpdate() method.
     */
    public void onComplicationUpdate(
            int complicationId, int dataType, ComplicationManager complicationManager) {
    }

    /*
     * Called when the complication has been deactivated. If you are updating the complication
     * manager outside of this class with updates, you will want to update your class to stop.
     */
    public void onComplicationDeactivated(int complicationId) {
    }
}
