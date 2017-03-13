package com.seapip.thomas.line_watchface.complications;

import android.support.wearable.complications.ComplicationData;

public class CanvasWatchFaceService extends android.support.wearable.watchface.CanvasWatchFaceService {
    public class Engine extends android.support.wearable.watchface.CanvasWatchFaceService.Engine {
        private int[] watchFaceComplicationIds;
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
        }

        @Override
        public void setActiveComplications(int... watchFaceComplicationIds) {
            this.watchFaceComplicationIds = watchFaceComplicationIds;
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            //Call onComplicationDataUpdate here in a certain interval
        }
    }
}