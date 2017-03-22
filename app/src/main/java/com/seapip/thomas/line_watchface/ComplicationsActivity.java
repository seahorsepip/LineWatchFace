package com.seapip.thomas.line_watchface;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

public class ComplicationsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new ComplicationsView(this));
    }

    class ComplicationsView extends View {
        public ComplicationsView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {

        }
    }
}
