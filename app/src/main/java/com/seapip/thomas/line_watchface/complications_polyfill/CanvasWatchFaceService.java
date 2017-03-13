package com.seapip.thomas.line_watchface.complications_polyfill;

import android.os.Handler;
import android.support.wearable.complications.ComplicationData;

public class CanvasWatchFaceService extends android.support.wearable.watchface.CanvasWatchFaceService {
    public class Engine extends android.support.wearable.watchface.CanvasWatchFaceService.Engine {
        ComplicationManager complicationManager;
        final Handler handler;
        Runnable run;

        public Engine() {
            super();
            complicationManager = new ComplicationManager(getBaseContext(), this);
            handler = new Handler();
            run = new Runnable()
            {
                @Override
                public void run()
                {
                    complicationManager.update();
                    handler.postDelayed(this, 1000);
                }
            };
            handler.postDelayed(run, 1000);
        }

        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
        }

        @Override
        public void setActiveComplications(int... watchFaceComplicationIds) {
            complicationManager.setActiveComplications(watchFaceComplicationIds);
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if(inAmbientMode) {
                handler.removeCallbacks(run);
            } else {
                handler.postDelayed(run, 1000);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            complicationManager.update();
        }
    }
}