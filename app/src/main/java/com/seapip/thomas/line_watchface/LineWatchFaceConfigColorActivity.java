package com.seapip.thomas.line_watchface;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wearable.view.WearableRecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * The watch-side config activity for {@link LineWatchFaceService}, which
 * allows for setting complications on the left and right of watch face.
 */
public class LineWatchFaceConfigColorActivity extends WearableActivity implements ConfigurationAdapter.ItemSelectedListener {

    private static final int PROVIDER_CHOOSER_REQUEST_CODE = 1;

    private ConfigurationAdapter mAdapter;
    private SharedPreferences mPrefs;
    private WearableRecyclerView mWearableRecyclerView;

    private String[] color_names;
    private int[] color_values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_watch_face_config);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        color_names = getResources().getStringArray(R.array.line_watch_face_color_names);
        TypedArray color_values_typed = getResources().obtainTypedArray(R.array.line_watch_face_color_values);
        color_values = new int[color_names.length];
        for (int i = 0; i < color_names.length; i++) {
            color_values[i] = color_values_typed.getColor(i, 0);
        }
        color_values_typed.recycle();

        mAdapter = new ConfigurationAdapter(getApplicationContext(), getConfigurationItems());
        mAdapter.setListener(this);
        mWearableRecyclerView = (WearableRecyclerView) findViewById(R.id.recycler_launcher_view);
        mWearableRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public void onItemSelected(int position) {

        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putString("setting_color_name", color_names[position]);
        mEditor.putInt("setting_color_value", color_values[position]);
        mEditor.commit();
        setResult(Activity.RESULT_OK, new Intent());
        finish();
    }

    private ArrayList<ConfigurationItemModel> getConfigurationItems() {
        ArrayList<ConfigurationItemModel> items = new ArrayList<>();
        for (int i = 0; i < color_names.length; i++) {
            final int color = color_values[i];
            final float radius = 20 * this.getResources().getDisplayMetrics().density;
            Drawable drawable = new Drawable() {
                @Override
                public void draw(@NonNull Canvas canvas) {
                    Paint backgroundPaint = new Paint();
                    backgroundPaint.setAntiAlias(true);
                    backgroundPaint.setColor(Color.argb(51, 255, 255, 255));
                    Paint colorPaint = new Paint();
                    colorPaint.setAntiAlias(true);
                    colorPaint.setColor(color);
                    canvas.drawCircle(radius, radius, radius, backgroundPaint);
                    canvas.drawCircle(radius, radius, radius / 2, colorPaint);
                }

                @Override
                public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
                }

                @Override
                public void setColorFilter(@Nullable ColorFilter colorFilter) {
                }

                @Override
                public int getOpacity() {
                    return PixelFormat.UNKNOWN;
                }
            };
            items.add(new ConfigurationItemModel(color_names[i],
                    drawable,
                    null,
                    null));
            ;
        }

        return items;
    }
}